package com.sunilsstartingseven.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation
import com.sunilsstartingseven.app.domain.model.*
import java.time.LocalDate

@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastInitial: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String get() = "$firstName $lastInitial."
}

@Entity(tableName = "games", indices = [Index("gameDate")])
data class Game(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roundNumber: Int,
    val gameDate: LocalDate,
    val status: GameStatus = GameStatus.DRAFT,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "attendance", primaryKeys = ["gameId", "playerId"],
    indices = [Index("playerId")],
    foreignKeys = [
        ForeignKey(entity = Game::class, parentColumns = ["id"], childColumns = ["gameId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Player::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.RESTRICT)
    ]
)
data class GameAttendance(val gameId: Long, val playerId: Long, val status: AttendanceStatus)

data class AttendanceWithPlayer(
    @Embedded val attendance: GameAttendance,
    @Relation(parentColumn = "playerId", entityColumn = "id") val player: Player
)

@Entity(
    tableName = "assignments", primaryKeys = ["gameId", "rotationIndex", "playerId"],
    indices = [Index("playerId"), Index(value = ["gameId", "rotationIndex", "position"], unique = true)],
    foreignKeys = [
        ForeignKey(entity = Game::class, parentColumns = ["id"], childColumns = ["gameId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Player::class, parentColumns = ["id"], childColumns = ["playerId"], onDelete = ForeignKey.RESTRICT)
    ]
)
data class Assignment(
    val gameId: Long,
    val rotationIndex: Int,
    val playerId: Long,
    val playingState: PlayingState,
    val position: PlayingPosition?
) {
    init {
        require(rotationIndex >= 0) { "Rotation index must be non-negative." }
        require((playingState == PlayingState.ON_FIELD) == (position != null)) { "Only an on-field assignment has a position." }
    }
}

data class AssignmentWithPlayer(
    @Embedded val assignment: Assignment,
    @Relation(parentColumn = "playerId", entityColumn = "id") val player: Player
)
