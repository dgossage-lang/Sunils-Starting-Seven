package com.sunilsstartingseven.app.data.backup

import androidx.room.withTransaction
import com.sunilsstartingseven.app.BuildConfig
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.media.MediaRepository
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.data.repository.DevelopmentSeedRepository
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.RotationAssignment
import com.sunilsstartingseven.app.domain.rotation.RotationValidator
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(
    private val db: AppDatabase,
    private val preferences: PreferenceRepository,
    private val media: MediaRepository,
    private val codec: BackupCodec = BackupCodec(),
    private val validator: BackupValidator = BackupValidator()
) {
    suspend fun createBackup(createdAt: OffsetDateTime = OffsetDateTime.now()): SunilsStartingSevenBackup =
        SunilsStartingSevenBackup(
            format = BACKUP_FORMAT,
            backupVersion = BACKUP_VERSION,
            createdAt = createdAt.toString(),
            appVersion = BuildConfig.VERSION_NAME,
            databaseSchemaVersion = BACKUP_SCHEMA_VERSION,
            players = db.playerDao().all().map { BackupPlayerDto(it.id, it.firstName, it.lastInitial, it.isActive, it.createdAt, it.photoPath) },
            games = db.gameDao().all().map { BackupGameDto(it.id, it.roundNumber, it.gameDate.toString(), it.status.name, it.notes, it.createdAt, it.photoPath) },
            attendance = db.rosterDao().allAttendance().map { BackupAttendanceDto(it.gameId, it.playerId, it.status.name) },
            assignments = db.rosterDao().allAssignments().map { BackupAssignmentDto(it.gameId, it.rotationIndex, it.playerId, it.playingState.name, it.position?.name) }
        )

    suspend fun createBackupJson(): String = codec.encode(createBackup())

    suspend fun writeBackupArchive(output: OutputStream) {
        val backup = createBackup()
        val manifest = codec.encode(backup)
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            val paths = (backup.players.mapNotNull { it.photoPath } + backup.games.mapNotNull { it.photoPath }).distinct()
            paths.forEach { path ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(media.readOwnedMedia(path))
                zip.closeEntry()
            }
        }
    }

    fun validateBytes(bytes: ByteArray): ValidatedBackup {
        if (bytes.size > MAX_ARCHIVE_BACKUP_BYTES) throw BackupException("This backup file is too large to restore.")
        return if (isZip(bytes)) validateArchive(bytes) else validateJson(bytes.toString(Charsets.UTF_8))
    }

    fun validateJson(text: String): ValidatedBackup {
        val backup = codec.decode(text).withoutV1Media()
        if (backup.backupVersion >= 2 && (backup.players.any { it.photoPath != null } || backup.games.any { it.photoPath != null })) {
            throw BackupException("Media backups must be restored from a .sssbackup archive.")
        }
        return validate(backup)
    }

    fun validate(backup: SunilsStartingSevenBackup, mediaFiles: Map<String, ByteArray> = emptyMap()): ValidatedBackup {
        validator.validate(backup, mediaFiles.keys)
        return ValidatedBackup(backup, backup.preview(), mediaFiles)
    }

    suspend fun restore(validated: ValidatedBackup) {
        val backup = validated.backup
        val stagedMedia = validated.media
        stagedMedia.forEach { (path, bytes) -> media.writeRestoredMedia(path, bytes) }
        db.withTransaction {
            db.rosterDao().deleteAllAssignments()
            db.rosterDao().deleteAllAttendance()
            db.gameDao().deleteAll()
            db.playerDao().deleteAll()
            db.playerDao().insert(backup.players.map { Player(it.id, it.firstName, it.lastInitial, it.isActive, it.createdAt, it.photoPath) })
            db.gameDao().insert(backup.games.map { Game(it.id, it.roundNumber, LocalDate.parse(it.gameDate), GameStatus.valueOf(it.status), it.notes, it.createdAt, it.photoPath) })
            db.rosterDao().insertAttendance(backup.attendance.map { GameAttendance(it.gameId, it.playerId, AttendanceStatus.valueOf(it.status)) })
            db.rosterDao().insertAssignments(backup.assignments.map { Assignment(it.gameId, it.rotationIndex, it.playerId, PlayingState.valueOf(it.playingState), it.position?.let(PlayingPosition::valueOf)) })
        }
        val keep = backup.players.mapNotNull { it.photoPath }.toSet() + backup.games.mapNotNull { it.photoPath }.toSet()
        media.deleteOrphanedMedia(keep)
        preferences.setDevelopmentSeedVersion(DevelopmentSeedRepository.DEVELOPMENT_SEED_VERSION)
    }

    private fun validateArchive(bytes: ByteArray): ValidatedBackup {
        val entries = mutableMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.replace('\\', '/')
                if (entry.isDirectory) continue
                if (!isSafeEntry(name)) throw BackupException("Backup contains an unsafe archive entry.")
                if (entries.containsKey(name)) throw BackupException("Backup contains duplicate archive entries.")
                val entryBytes = zip.readEntryCapped(MAX_ARCHIVE_ENTRY_BYTES)
                if (entryBytes.size > MAX_ARCHIVE_ENTRY_BYTES) throw BackupException("Backup contains an oversized media entry.")
                total += entryBytes.size
                if (total > MAX_ARCHIVE_TOTAL_UNCOMPRESSED_BYTES) throw BackupException("Backup is too large to restore safely.")
                entries[name] = entryBytes
            }
        }
        val manifestBytes = entries.remove("manifest.json") ?: throw BackupException("Backup archive is missing manifest.json.")
        val backup = codec.decode(manifestBytes.toString(Charsets.UTF_8))
        val referenced = (backup.players.mapNotNull { it.photoPath } + backup.games.mapNotNull { it.photoPath }).toSet()
        val unexpected = entries.keys - referenced
        if (unexpected.isNotEmpty()) throw BackupException("Backup contains unexpected media files.")
        val missing = referenced - entries.keys
        if (missing.isNotEmpty()) throw BackupException("Backup is missing referenced media files.")
        return validate(backup, entries)
    }

    private fun isZip(bytes: ByteArray): Boolean = bytes.size >= 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()

    private fun isSafeEntry(name: String): Boolean {
        if (name == "manifest.json") return true
        if (name.startsWith("/") || name.contains("..") || name.contains(':')) return false
        return media.isSafeMediaPath(name)
    }

    private fun InputStream.readEntryCapped(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            if (output.size() > maxBytes) break
        }
        return output.toByteArray()
    }

    private fun SunilsStartingSevenBackup.withoutV1Media(): SunilsStartingSevenBackup = if (backupVersion == 1) copy(
        databaseSchemaVersion = 1,
        players = players.map { it.copy(photoPath = null) },
        games = games.map { it.copy(photoPath = null) }
    ) else this
}

class BackupValidator(private val rosterValidator: RotationValidator = RotationValidator()) {
    fun validate(backup: SunilsStartingSevenBackup, mediaPaths: Set<String> = emptySet()) {
        if (backup.format != BACKUP_FORMAT) throw BackupException("This doesn't appear to be a Sunil's Starting Seven backup.")
        if (backup.backupVersion > BACKUP_VERSION) throw BackupException("This backup was created by a newer backup format and can't be restored by this version of the app.")
        if (backup.backupVersion !in 1..BACKUP_VERSION) throw BackupException("This backup format is not supported.")
        val expectedSchema = if (backup.backupVersion == 1) 1 else BACKUP_SCHEMA_VERSION
        if (backup.databaseSchemaVersion != expectedSchema) throw BackupException("This backup uses an unsupported database schema.")
        runCatching { OffsetDateTime.parse(backup.createdAt) }.getOrElse { throw BackupException("Backup has an invalid createdAt timestamp.") }

        val playerIds = uniqueIds(backup.players.map { it.id }, "player")
        backup.players.forEach { player ->
            if (player.id <= 0) throw BackupException("Backup contains an invalid player ID.")
            if (player.firstName.isBlank()) throw BackupException("Backup contains a player without a first name.")
            if (player.lastInitial.length > 1) throw BackupException("Backup contains an invalid last initial.")
            validateMediaPath(player.photoPath, "players", mediaPaths)
        }
        val gameIds = uniqueIds(backup.games.map { it.id }, "game")
        backup.games.forEach { game ->
            if (game.id <= 0 || game.roundNumber <= 0) throw BackupException("Backup contains an invalid game.")
            parseGameStatus(game.status)
            runCatching { LocalDate.parse(game.gameDate) }.getOrElse { throw BackupException("Backup contains an invalid game date.") }
            validateMediaPath(game.photoPath, "games", mediaPaths)
        }
        val attendanceKeys = mutableSetOf<Pair<Long, Long>>()
        backup.attendance.forEach { row ->
            if (row.gameId !in gameIds) throw BackupException("Backup attendance references a missing game.")
            if (row.playerId !in playerIds) throw BackupException("Backup attendance references a missing player.")
            if (!attendanceKeys.add(row.gameId to row.playerId)) throw BackupException("Backup contains duplicate attendance.")
            parseAttendanceStatus(row.status)
        }
        val assignmentKeys = mutableSetOf<Triple<Long, Int, Long>>()
        backup.assignments.forEach { row ->
            if (row.gameId !in gameIds) throw BackupException("Backup assignment references a missing game.")
            if (row.playerId !in playerIds) throw BackupException("Backup assignment references a missing player.")
            if (row.rotationIndex !in 0..3) throw BackupException("Backup contains an invalid rotation index.")
            val state = parsePlayingState(row.playingState)
            val position = row.position?.let(::parsePosition)
            if ((state == PlayingState.ON_FIELD) != (position != null)) throw BackupException("Backup contains an invalid assignment state.")
            if (!assignmentKeys.add(Triple(row.gameId, row.rotationIndex, row.playerId))) throw BackupException("Backup contains duplicate assignments.")
        }
        validateReadyAndPlayedRosters(backup)
    }

    private fun validateMediaPath(path: String?, expectedDir: String, mediaPaths: Set<String>) {
        if (path == null) return
        if (!path.startsWith("media/$expectedDir/") || !path.endsWith(".jpg") || path.contains("..") || path.contains('\\') || path.contains(':')) throw BackupException("Backup contains an unsafe media reference.")
        if (mediaPaths.isNotEmpty() && path !in mediaPaths) throw BackupException("Backup is missing referenced media files.")
    }

    private fun validateReadyAndPlayedRosters(backup: SunilsStartingSevenBackup) {
        val assignmentsByGame = backup.assignments.groupBy { it.gameId }
        val attendanceByGame = backup.attendance.groupBy { it.gameId }
        backup.games.filter { parseGameStatus(it.status) in setOf(GameStatus.READY, GameStatus.PLAYED) }.forEach { game ->
            val available = attendanceByGame[game.id].orEmpty().filter { parseAttendanceStatus(it.status) == AttendanceStatus.AVAILABLE }.map { it.playerId }.toSet()
            if (assignmentsByGame[game.id].orEmpty().isEmpty()) return@forEach
            val away = attendanceByGame[game.id].orEmpty().filter { parseAttendanceStatus(it.status) == AttendanceStatus.AWAY }.map { it.playerId }.toSet()
            val rows = assignmentsByGame[game.id].orEmpty().map { RotationAssignment(it.playerId, it.rotationIndex, parsePlayingState(it.playingState), it.position?.let(::parsePosition)) }
            val validation = rosterValidator.validate(rows, available, away)
            if (!validation.valid) throw BackupException(validation.errors.joinToString(" "))
        }
    }

    private fun uniqueIds(ids: List<Long>, label: String): Set<Long> {
        val set = ids.toSet()
        if (set.size != ids.size) throw BackupException("Backup contains duplicate ${label} IDs.")
        return set
    }

    private fun parseGameStatus(value: String): GameStatus = runCatching { GameStatus.valueOf(value) }.getOrElse { throw BackupException("Backup contains an unknown game status.") }
    private fun parseAttendanceStatus(value: String): AttendanceStatus = runCatching { AttendanceStatus.valueOf(value) }.getOrElse { throw BackupException("Backup contains an unknown attendance status.") }
    private fun parsePlayingState(value: String): PlayingState = runCatching { PlayingState.valueOf(value) }.getOrElse { throw BackupException("Backup contains an unknown playing state.") }
    private fun parsePosition(value: String): PlayingPosition = runCatching { PlayingPosition.valueOf(value) }.getOrElse { throw BackupException("Backup contains an unknown playing position.") }
}
