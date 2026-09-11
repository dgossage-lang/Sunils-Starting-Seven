package com.sunilsstartingseven.app.data.backup

import androidx.room.withTransaction
import com.sunilsstartingseven.app.BuildConfig
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.data.repository.DevelopmentSeedRepository
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.RotationAssignment
import com.sunilsstartingseven.app.domain.rotation.RotationValidator
import java.time.LocalDate
import java.time.OffsetDateTime

class BackupRepository(
    private val db: AppDatabase,
    private val preferences: PreferenceRepository,
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
            players = db.playerDao().all().map { BackupPlayerDto(it.id, it.firstName, it.lastInitial, it.isActive, it.createdAt) },
            games = db.gameDao().all().map { BackupGameDto(it.id, it.roundNumber, it.gameDate.toString(), it.status.name, it.notes, it.createdAt) },
            attendance = db.rosterDao().allAttendance().map { BackupAttendanceDto(it.gameId, it.playerId, it.status.name) },
            assignments = db.rosterDao().allAssignments().map { BackupAssignmentDto(it.gameId, it.rotationIndex, it.playerId, it.playingState.name, it.position?.name) }
        )

    suspend fun createBackupJson(): String = codec.encode(createBackup())

    fun validateJson(text: String): ValidatedBackup = validate(codec.decode(text))

    fun validate(backup: SunilsStartingSevenBackup): ValidatedBackup {
        validator.validate(backup)
        return ValidatedBackup(backup, backup.preview())
    }

    suspend fun restore(validated: ValidatedBackup) {
        val backup = validated.backup
        db.withTransaction {
            db.rosterDao().deleteAllAssignments()
            db.rosterDao().deleteAllAttendance()
            db.gameDao().deleteAll()
            db.playerDao().deleteAll()
            db.playerDao().insert(backup.players.map { Player(it.id, it.firstName, it.lastInitial, it.isActive, it.createdAt) })
            db.gameDao().insert(backup.games.map { Game(it.id, it.roundNumber, LocalDate.parse(it.gameDate), GameStatus.valueOf(it.status), it.notes, it.createdAt) })
            db.rosterDao().insertAttendance(backup.attendance.map { GameAttendance(it.gameId, it.playerId, AttendanceStatus.valueOf(it.status)) })
            db.rosterDao().insertAssignments(backup.assignments.map { Assignment(it.gameId, it.rotationIndex, it.playerId, PlayingState.valueOf(it.playingState), it.position?.let(PlayingPosition::valueOf)) })
        }
        preferences.setDevelopmentSeedVersion(DevelopmentSeedRepository.DEVELOPMENT_SEED_VERSION)
    }
}

class BackupValidator(private val rosterValidator: RotationValidator = RotationValidator()) {
    fun validate(backup: SunilsStartingSevenBackup) {
        if (backup.format != BACKUP_FORMAT) throw BackupException("This doesn't appear to be a Sunil's Starting Seven backup.")
        if (backup.backupVersion > BACKUP_VERSION) throw BackupException("This backup was created by a newer backup format and can't be restored by this version of the app.")
        if (backup.backupVersion != BACKUP_VERSION) throw BackupException("This backup format is not supported.")
        if (backup.databaseSchemaVersion != BACKUP_SCHEMA_VERSION) throw BackupException("This backup uses an unsupported database schema.")
        runCatching { OffsetDateTime.parse(backup.createdAt) }.getOrElse { throw BackupException("Backup has an invalid createdAt timestamp.") }

        val playerIds = uniqueIds(backup.players.map { it.id }, "player")
        backup.players.forEach { player ->
            if (player.id <= 0) throw BackupException("Backup contains an invalid player ID.")
            if (player.firstName.isBlank()) throw BackupException("Backup contains a player without a first name.")
            if (player.lastInitial.length > 1) throw BackupException("Backup contains an invalid last initial.")
        }
        val gameIds = uniqueIds(backup.games.map { it.id }, "game")
        backup.games.forEach { game ->
            if (game.id <= 0 || game.roundNumber <= 0) throw BackupException("Backup contains an invalid game.")
            parseGameStatus(game.status)
            runCatching { LocalDate.parse(game.gameDate) }.getOrElse { throw BackupException("Backup contains an invalid game date.") }
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

    private fun validateReadyAndPlayedRosters(backup: SunilsStartingSevenBackup) {
        val assignmentsByGame = backup.assignments.groupBy { it.gameId }
        val attendanceByGame = backup.attendance.groupBy { it.gameId }
        backup.games.filter { parseGameStatus(it.status) in setOf(GameStatus.READY, GameStatus.PLAYED) }.forEach { game ->
            val available = attendanceByGame[game.id].orEmpty().filter { parseAttendanceStatus(it.status) == AttendanceStatus.AVAILABLE }.map { it.playerId }.toSet()
            if (assignmentsByGame[game.id].orEmpty().isEmpty()) return@forEach
            val away = attendanceByGame[game.id].orEmpty().filter { parseAttendanceStatus(it.status) == AttendanceStatus.AWAY }.map { it.playerId }.toSet()
            val rows = assignmentsByGame[game.id].orEmpty().map {
                RotationAssignment(it.playerId, it.rotationIndex, parsePlayingState(it.playingState), it.position?.let(::parsePosition))
            }
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
