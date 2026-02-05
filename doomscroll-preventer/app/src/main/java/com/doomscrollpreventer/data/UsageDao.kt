package com.doomscrollpreventer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: UsageEntity)

    @Update
    suspend fun update(usage: UsageEntity)

    // --- Single queries ---

    @Query("SELECT * FROM usage_sessions WHERE packageName = :packageName AND dateKey = :dateKey LIMIT 1")
    suspend fun getUsageForAppOnDate(packageName: String, dateKey: String): UsageEntity?

    @Query("SELECT * FROM usage_sessions WHERE dateKey = :dateKey")
    suspend fun getUsageForDate(dateKey: String): List<UsageEntity>

    // --- Dashboard: Today ---

    @Query("SELECT * FROM usage_sessions WHERE dateKey = :dateKey ORDER BY totalScrollTimeMs DESC")
    fun getTodayUsageLive(dateKey: String): LiveData<List<UsageEntity>>

    @Query("SELECT COALESCE(SUM(totalScrollTimeMs), 0) FROM usage_sessions WHERE dateKey = :dateKey")
    fun getTotalTimeForDateLive(dateKey: String): LiveData<Long>

    // --- Dashboard: Date range ---

    @Query("SELECT * FROM usage_sessions WHERE dateKey >= :startDate AND dateKey <= :endDate ORDER BY dateKey ASC, totalScrollTimeMs DESC")
    fun getUsageInRangeLive(startDate: String, endDate: String): LiveData<List<UsageEntity>>

    @Query("SELECT * FROM usage_sessions WHERE dateKey >= :startDate AND dateKey <= :endDate ORDER BY dateKey ASC, totalScrollTimeMs DESC")
    suspend fun getUsageInRange(startDate: String, endDate: String): List<UsageEntity>

    // --- Aggregations ---

    @Query("""
        SELECT appDisplayName, SUM(totalScrollTimeMs) as totalScrollTimeMs
        FROM usage_sessions
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        GROUP BY packageName
        ORDER BY totalScrollTimeMs DESC
    """)
    suspend fun getAggregatedUsageByApp(startDate: String, endDate: String): List<AppUsageSummary>

    @Query("""
        SELECT dateKey, SUM(totalScrollTimeMs) as totalMs
        FROM usage_sessions
        WHERE dateKey >= :startDate AND dateKey <= :endDate
        GROUP BY dateKey
        ORDER BY dateKey ASC
    """)
    suspend fun getDailyTotals(startDate: String, endDate: String): List<DailyTotal>

    // --- Cleanup ---

    @Query("DELETE FROM usage_sessions WHERE dateKey < :beforeDate")
    suspend fun deleteOldData(beforeDate: String)
}

data class AppUsageSummary(
    val appDisplayName: String,
    val totalScrollTimeMs: Long
)

data class DailyTotal(
    val dateKey: String,
    val totalMs: Long
)
