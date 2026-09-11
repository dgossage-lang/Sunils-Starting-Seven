package com.sunilsstartingseven.app.data.local

import androidx.room.*
import com.sunilsstartingseven.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Converters {
    @TypeConverter fun date(value: Long): LocalDate = LocalDate.ofEpochDay(value)
    @TypeConverter fun epochDay(value: LocalDate): Long = value.toEpochDay()
    @TypeConverter fun gameStatus(value: String): GameStatus = GameStatus.valueOf(value)
    @TypeConverter fun gameStatusName(value: GameStatus): String = value.name
    @TypeConverter fun attendanceStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)
    @TypeConverter fun attendanceStatusName(value: AttendanceStatus): String = value.name
    @TypeConverter fun playingState(value: String): PlayingState = PlayingState.valueOf(value)
    @TypeConverter fun playingStateName(value: PlayingState): String = value.name
    @TypeConverter fun position(value: String?): PlayingPosition? = value?.let(PlayingPosition::valueOf)
    @TypeConverter fun positionName(value: PlayingPosition?): String? = value?.name
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY isActive DESC, firstName COLLATE NOCASE, lastInitial COLLATE NOCASE, id")
    fun observeAll(): Flow<List<Player>>
    @Query("SELECT * FROM players WHERE id = :id") suspend fun get(id: Long): Player?
    @Insert suspend fun insert(player: Player): Long
    @Query("UPDATE players SET firstName = :firstName, lastInitial = :lastInitial WHERE id = :id")
    suspend fun updateName(id: Long, firstName: String, lastInitial: String): Int
    @Query("UPDATE players SET isActive = :active WHERE id = :id") suspend fun setActive(id: Long, active: Boolean): Int
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY gameDate DESC, roundNumber DESC, id DESC") fun observeAll(): Flow<List<Game>>
    @Query("SELECT * FROM games WHERE id = :id") suspend fun get(id: Long): Game?
    @Insert suspend fun insert(game: Game): Long
}

@Dao
interface RosterDao {
    @Query("SELECT * FROM attendance WHERE gameId = :gameId ORDER BY playerId") fun observeAttendance(gameId: Long): Flow<List<GameAttendance>>
    @Query("SELECT * FROM assignments WHERE gameId = :gameId ORDER BY rotationIndex, playerId") fun observeAssignments(gameId: Long): Flow<List<Assignment>>
    @Insert suspend fun insertAttendance(attendance: GameAttendance)
    @Insert suspend fun insertAssignment(assignment: Assignment)
}

@Database(entities = [Player::class, Game::class, GameAttendance::class, Assignment::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun rosterDao(): RosterDao
}
