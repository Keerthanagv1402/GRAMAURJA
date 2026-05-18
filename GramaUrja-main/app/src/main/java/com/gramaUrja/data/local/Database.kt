package com.gramaUrja.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entity ────────────────────────────────────────────────────────────────────
@Entity(tableName = "power_history")
data class PowerHistoryEntity(
    @PrimaryKey val id: String,
    val zoneId: String,
    val status: String,
    val timestamp: Long,
    val reporterId: String
)

// ── DAO ───────────────────────────────────────────────────────────────────────
@Dao
interface PowerHistoryDao {

    @Query("SELECT * FROM power_history WHERE zoneId = :zoneId ORDER BY timestamp DESC LIMIT 30")
    fun observeHistory(zoneId: String): Flow<List<PowerHistoryEntity>>

    @Query("SELECT * FROM power_history WHERE zoneId = :zoneId ORDER BY timestamp DESC LIMIT 30")
    suspend fun getHistory(zoneId: String): List<PowerHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<PowerHistoryEntity>)

    @Query("DELETE FROM power_history WHERE zoneId = :zoneId")
    suspend fun clearZone(zoneId: String)
}

// ── Database ──────────────────────────────────────────────────────────────────
@Database(entities = [PowerHistoryEntity::class], version = 1, exportSchema = false)
abstract class GramaUrjaDatabase : RoomDatabase() {
    abstract fun powerHistoryDao(): PowerHistoryDao
}
