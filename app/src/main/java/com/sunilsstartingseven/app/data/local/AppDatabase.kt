package com.sunilsstartingseven.app.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunilsstartingseven.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class Converters {
    @TypeConverter fun fieldPositions(value: String): List<PlayingPosition> = PositionCodec.decodeLayout(value)
    @TypeConverter fun fieldPositionNames(value: List<PlayingPosition>): String = PositionCodec.encodeLayout(value)
    @TypeConverter fun date(value: Long): LocalDate = LocalDate.ofEpochDay(value)
    @TypeConverter fun epochDay(value: LocalDate): Long = value.toEpochDay()
    @TypeConverter fun gameStatus(value: String): GameStatus = GameStatus.valueOf(value)
    @TypeConverter fun gameStatusName(value: GameStatus): String = value.name
    @TypeConverter fun attendanceStatus(value: String): AttendanceStatus = AttendanceStatus.valueOf(value)
    @TypeConverter fun attendanceStatusName(value: AttendanceStatus): String = value.name
    @TypeConverter fun playingState(value: String): PlayingState = PlayingState.valueOf(value)
    @TypeConverter fun playingStateName(value: PlayingState): String = value.name
    @TypeConverter fun position(value: String?): PlayingPosition? = value?.let(PlayingPosition::valueOf)
    @TypeConverter fun positionName(value: PlayingPosition?): String? = value?.let(PositionCodec::encode)
}

@Dao
interface SeasonDao {
    @Query("SELECT * FROM seasons ORDER BY createdAt DESC, id DESC") fun observeAll(): Flow<List<Season>>
    @Query("SELECT * FROM seasons ORDER BY id") suspend fun all(): List<Season>
    @Query("SELECT * FROM seasons WHERE id = :id") suspend fun get(id: Long): Season?
    @Insert suspend fun insert(season: Season): Long
    @Insert suspend fun insert(seasons: List<Season>)
    @Query("UPDATE seasons SET name = :name WHERE id = :id") suspend fun rename(id: Long, name: String): Int
    @Query("DELETE FROM seasons") suspend fun deleteAll()
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY isActive DESC, firstName COLLATE NOCASE, lastInitial COLLATE NOCASE, id")
    fun observeAll(): Flow<List<Player>>
    @Query("SELECT * FROM players ORDER BY id") suspend fun all(): List<Player>
    @Query("SELECT * FROM players WHERE id = :id") suspend fun get(id: Long): Player?
    @Query("SELECT * FROM players WHERE isActive = 1 ORDER BY firstName COLLATE NOCASE, lastInitial COLLATE NOCASE, id")
    suspend fun activePlayers(): List<Player>
    @Insert suspend fun insert(player: Player): Long
    @Insert suspend fun insert(players: List<Player>)
    @Query("UPDATE players SET firstName = :firstName, lastInitial = :lastInitial, carryFairnessAcrossSeasons = :carryFairnessAcrossSeasons WHERE id = :id")
    suspend fun updateName(id: Long, firstName: String, lastInitial: String, carryFairnessAcrossSeasons: Boolean = false): Int
    @Query("UPDATE players SET photoPath = :photoPath WHERE id = :id")
    suspend fun updatePhotoPath(id: Long, photoPath: String?): Int
    @Query("UPDATE players SET isActive = :active WHERE id = :id") suspend fun setActive(id: Long, active: Boolean): Int
    @Query("DELETE FROM players") suspend fun deleteAll()
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games ORDER BY createdAt DESC, id DESC LIMIT 1") suspend fun latest(): Game?
    @Query("SELECT * FROM games WHERE seasonId = :seasonId ORDER BY createdAt DESC, id DESC LIMIT 1") suspend fun latestInSeason(seasonId: Long): Game?
    @Query("UPDATE games SET fieldPositions = :positions, positionOptions = :options WHERE id = :id") suspend fun updateLayout(id: Long, positions: List<PlayingPosition>, options: List<PlayingPosition>): Int
    @Query("SELECT * FROM games ORDER BY gameDate DESC, roundNumber DESC, id DESC") fun observeAll(): Flow<List<Game>>
    @Query("SELECT * FROM games ORDER BY id") suspend fun all(): List<Game>
    @Query("SELECT * FROM games WHERE id = :id") suspend fun get(id: Long): Game?
    @Insert suspend fun insert(game: Game): Long
    @Insert suspend fun insert(games: List<Game>)
    @Query("UPDATE games SET roundNumber = :roundNumber, gameDate = :date, notes = :notes WHERE id = :id AND status = 'DRAFT'")
    suspend fun updateDraftDetails(id: Long, roundNumber: Int, date: LocalDate, notes: String): Int
    @Query("UPDATE games SET status = :status WHERE id = :id") suspend fun updateStatus(id: Long, status: GameStatus): Int
    @Query("UPDATE games SET photoPath = :photoPath WHERE id = :id") suspend fun updatePhotoPath(id: Long, photoPath: String?): Int
    @Query("SELECT * FROM games WHERE status = 'PLAYED' ORDER BY gameDate, roundNumber, id") suspend fun playedGames(): List<Game>
    @Query("DELETE FROM games") suspend fun deleteAll()
}

@Dao
interface RosterDao {
    @Query("SELECT * FROM attendance WHERE gameId = :gameId ORDER BY playerId") fun observeAttendance(gameId: Long): Flow<List<GameAttendance>>
    @Transaction
    @Query("SELECT * FROM attendance ORDER BY gameId, playerId")
    fun observeAllAttendanceWithPlayers(): Flow<List<AttendanceWithPlayer>>
    @Transaction
    @Query("SELECT * FROM attendance WHERE gameId = :gameId ORDER BY playerId")
    fun observeAttendanceWithPlayers(gameId: Long): Flow<List<AttendanceWithPlayer>>
    @Query("SELECT COUNT(*) FROM attendance WHERE gameId = :gameId")
    suspend fun attendanceCount(gameId: Long): Int
    @Query("SELECT * FROM players WHERE isActive = 1 AND id NOT IN (SELECT playerId FROM attendance WHERE gameId = :gameId) ORDER BY firstName COLLATE NOCASE, lastInitial COLLATE NOCASE, id")
    suspend fun activePlayersMissingFromAttendance(gameId: Long): List<Player>
    @Query("SELECT * FROM assignments WHERE gameId = :gameId ORDER BY rotationIndex, playerId") fun observeAssignments(gameId: Long): Flow<List<Assignment>>
    @Query("SELECT * FROM attendance WHERE gameId = :gameId ORDER BY playerId")
    suspend fun getAttendance(gameId: Long): List<GameAttendance>
    @Query("SELECT * FROM attendance ORDER BY gameId, playerId") suspend fun allAttendance(): List<GameAttendance>
    @Transaction
    @Query("SELECT * FROM attendance WHERE gameId = :gameId ORDER BY playerId")
    suspend fun getAttendanceWithPlayers(gameId: Long): List<AttendanceWithPlayer>
    @Query("SELECT * FROM assignments WHERE gameId = :gameId ORDER BY rotationIndex, playerId")
    suspend fun getAssignments(gameId: Long): List<Assignment>
    @Query("SELECT * FROM assignments ORDER BY gameId, rotationIndex, playerId") suspend fun allAssignments(): List<Assignment>
    @Query("SELECT * FROM assignments WHERE gameId IN (SELECT id FROM games WHERE status = 'PLAYED') ORDER BY gameId, rotationIndex, playerId")
    suspend fun playedAssignments(): List<Assignment>
    @Query("SELECT COUNT(*) FROM assignments WHERE gameId = :gameId")
    suspend fun assignmentCount(gameId: Long): Int
    @Transaction
    @Query("SELECT * FROM assignments ORDER BY gameId, rotationIndex, playerId")
    fun observeAllAssignmentsWithPlayers(): Flow<List<AssignmentWithPlayer>>
    @Transaction
    @Query("SELECT * FROM assignments WHERE gameId = :gameId ORDER BY rotationIndex, playerId")
    fun observeAssignmentsWithPlayers(gameId: Long): Flow<List<AssignmentWithPlayer>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAttendance(attendance: GameAttendance): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAttendance(attendance: List<GameAttendance>)
    @Query("UPDATE attendance SET status = :status WHERE gameId = :gameId AND playerId = :playerId")
    suspend fun updateAttendanceStatus(gameId: Long, playerId: Long, status: AttendanceStatus): Int
    @Insert suspend fun insertAssignment(assignment: Assignment)
    @Insert suspend fun insertAssignments(assignments: List<Assignment>)
    @Query("DELETE FROM assignments WHERE gameId = :gameId")
    suspend fun deleteAssignmentsForGame(gameId: Long)
    @Query("DELETE FROM assignments")
    suspend fun deleteAllAssignments()
    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance()
}

@Database(entities = [Player::class, Season::class, Game::class, GameAttendance::class, Assignment::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun seasonDao(): SeasonDao
    abstract fun playerDao(): PlayerDao
    abstract fun gameDao(): GameDao
    abstract fun rosterDao(): RosterDao

    companion object {
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS seasons (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("INSERT INTO seasons (id, name, createdAt) SELECT 1, 'Earlier games', 0 WHERE EXISTS (SELECT 1 FROM games)")
                db.execSQL("ALTER TABLE games ADD COLUMN seasonId INTEGER REFERENCES seasons(id) ON DELETE RESTRICT")
                db.execSQL("ALTER TABLE games ADD COLUMN fieldPositions TEXT NOT NULL DEFAULT 'STRIKER,LEFT_WING,MID,RIGHT_WING,DEFENDER_1,DEFENDER_2,GOALKEEPER'")
                db.execSQL("ALTER TABLE games ADD COLUMN positionOptions TEXT NOT NULL DEFAULT 'STRIKER,LEFT_WING,MID,RIGHT_WING,DEFENDER_1,DEFENDER_2,GOALKEEPER'")
                db.execSQL("ALTER TABLE players ADD COLUMN carryFairnessAcrossSeasons INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE games SET seasonId = 1")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_games_seasonId ON games (seasonId)")
            }
        }
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE players ADD COLUMN photoPath TEXT")
                db.execSQL("ALTER TABLE games ADD COLUMN photoPath TEXT")
            }
        }
    }
}
