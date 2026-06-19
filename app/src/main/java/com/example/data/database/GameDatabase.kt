package com.example.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ---------------- Entities ----------------

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rank: Int,
    val name: String,
    val score: Int,
    val kills: Int,
    val deaths: Int,
    val winRate: Float,
    val tier: String, // e.g., "Grandmaster", "Diamond", "Bronze"
    val isLocalPlayer: Boolean = false
)

@Entity(tableName = "highlights")
data class MatchHighlight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val gameMode: String, // "Local PVP" or "Online Arena"
    val result: String, // "Victory" or "Defeat"
    val scoreSummary: String, // e.g., "5 - 3" or "10 - 2"
    val timestamp: Long = System.currentTimeMillis(),
    val narrative: String, // Generative AI Commentary
    val matchEventsText: String // JSON event list / stats
)

// ---------------- DAO ----------------

@Dao
interface GameDao {
    // Leaderboard
    @Query("SELECT * FROM leaderboard ORDER BY score DESC")
    fun getLeaderboard(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntry(entry: LeaderboardEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaderboardEntries(entries: List<LeaderboardEntry>)

    @Query("DELETE FROM leaderboard WHERE isLocalPlayer = 1")
    suspend fun deleteLocalPlayerEntries()

    // Highlights
    @Query("SELECT * FROM highlights ORDER BY timestamp DESC")
    fun getAllHighlights(): Flow<List<MatchHighlight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: MatchHighlight)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlight(id: Int)
}

// ---------------- Database ----------------

@Database(entities = [LeaderboardEntry::class, MatchHighlight::class], version = 1, exportSchema = false)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "neon_laser_game_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ---------------- Repository ----------------

class GameRepository(private val gameDao: GameDao) {
    val leaderboard: Flow<List<LeaderboardEntry>> = gameDao.getLeaderboard()
    val highlights: Flow<List<MatchHighlight>> = gameDao.getAllHighlights()

    suspend fun insertLeaderboard(entry: LeaderboardEntry) {
        gameDao.insertLeaderboardEntry(entry)
    }

    suspend fun insertLeaderboards(entries: List<LeaderboardEntry>) {
        gameDao.insertLeaderboardEntries(entries)
    }

    suspend fun insertHighlight(highlight: MatchHighlight) {
        gameDao.insertHighlight(highlight)
    }

    suspend fun deleteHighlight(id: Int) {
        gameDao.deleteHighlight(id)
    }

    // Initialize with standard server players for Leaderboard if empty
    suspend fun seedLeaderboardIfEmpty(currentList: List<LeaderboardEntry>) {
        if (currentList.isEmpty()) {
            val seedData = listOf(
                LeaderboardEntry(rank = 1, name = "CosmicOverlord", score = 9850, kills = 210, deaths = 45, winRate = 0.82f, tier = "Apex Master"),
                LeaderboardEntry(rank = 2, name = "PhasedFighter", score = 8900, kills = 185, deaths = 52, winRate = 0.78f, tier = "Apex Master"),
                LeaderboardEntry(rank = 3, name = "LightShield", score = 8120, kills = 154, deaths = 48, winRate = 0.76f, tier = "Grandmaster"),
                LeaderboardEntry(rank = 4, name = "QuantumRebel", score = 7640, kills = 142, deaths = 61, winRate = 0.70f, tier = "Grandmaster"),
                LeaderboardEntry(rank = 5, name = "VelocityX", score = 6980, kills = 130, deaths = 58, winRate = 0.69f, tier = "Diamond"),
                LeaderboardEntry(rank = 6, name = "BeamPulse", score = 6120, kills = 110, deaths = 73, winRate = 0.60f, tier = "Diamond"),
                LeaderboardEntry(rank = 7, name = "VortexPulse", score = 5400, kills = 95, deaths = 80, winRate = 0.54f, tier = "Platinum"),
                LeaderboardEntry(rank = 8, name = "CyberWasp", score = 4950, kills = 81, deaths = 88, winRate = 0.48f, tier = "Platinum"),
                LeaderboardEntry(rank = 9, name = "PhotonDash", score = 3800, kills = 62, deaths = 90, winRate = 0.41f, tier = "Gold"),
                LeaderboardEntry(rank = 10, name = "ChronoScythe", score = 2500, kills = 38, deaths = 76, winRate = 0.33f, tier = "Silver")
            )
            gameDao.insertLeaderboardEntries(seedData)
        }
    }
}
