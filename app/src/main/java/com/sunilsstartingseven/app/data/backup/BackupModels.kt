package com.sunilsstartingseven.app.data.backup

import com.sunilsstartingseven.app.domain.model.GameStatus
import java.time.OffsetDateTime

const val BACKUP_FORMAT = "sunils-starting-seven-backup"
const val BACKUP_VERSION = 1
const val BACKUP_SCHEMA_VERSION = 1
const val MAX_BACKUP_BYTES = 10 * 1024 * 1024

data class BackupPlayerDto(val id: Long, val firstName: String, val lastInitial: String, val isActive: Boolean, val createdAt: Long)
data class BackupGameDto(val id: Long, val roundNumber: Int, val gameDate: String, val status: String, val notes: String, val createdAt: Long)
data class BackupAttendanceDto(val gameId: Long, val playerId: Long, val status: String)
data class BackupAssignmentDto(val gameId: Long, val rotationIndex: Int, val playerId: Long, val playingState: String, val position: String?)

data class SunilsStartingSevenBackup(
    val format: String,
    val backupVersion: Int,
    val createdAt: String,
    val appVersion: String,
    val databaseSchemaVersion: Int,
    val players: List<BackupPlayerDto>,
    val games: List<BackupGameDto>,
    val attendance: List<BackupAttendanceDto>,
    val assignments: List<BackupAssignmentDto>
)

data class BackupPreview(
    val createdAt: OffsetDateTime?,
    val appVersion: String,
    val backupVersion: Int,
    val playerCount: Int,
    val activePlayerCount: Int,
    val inactivePlayerCount: Int,
    val gameCount: Int,
    val playedCount: Int,
    val readyCount: Int,
    val draftCount: Int,
    val cancelledCount: Int
) {
    val summaryLine: String get() = "$playerCount players - $gameCount games"
}

data class ValidatedBackup(val backup: SunilsStartingSevenBackup, val preview: BackupPreview)

class BackupException(message: String) : Exception(message)

fun SunilsStartingSevenBackup.preview(): BackupPreview {
    val counts = games.groupingBy { runCatching { GameStatus.valueOf(it.status) }.getOrNull() }.eachCount()
    return BackupPreview(
        createdAt = runCatching { OffsetDateTime.parse(createdAt) }.getOrNull(),
        appVersion = appVersion,
        backupVersion = backupVersion,
        playerCount = players.size,
        activePlayerCount = players.count { it.isActive },
        inactivePlayerCount = players.count { !it.isActive },
        gameCount = games.size,
        playedCount = counts[GameStatus.PLAYED] ?: 0,
        readyCount = counts[GameStatus.READY] ?: 0,
        draftCount = counts[GameStatus.DRAFT] ?: 0,
        cancelledCount = counts[GameStatus.CANCELLED] ?: 0
    )
}
