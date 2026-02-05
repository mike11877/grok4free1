package com.doomscrollpreventer.data

import androidx.lifecycle.LiveData
import com.doomscrollpreventer.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class UsageRepository(private val usageDao: UsageDao) {

    // --- Today ---

    fun getTodayUsage(): LiveData<List<UsageEntity>> {
        return usageDao.getTodayUsageLive(TimeUtils.todayDateString())
    }

    fun getTodayTotal(): LiveData<Long> {
        return usageDao.getTotalTimeForDateLive(TimeUtils.todayDateString())
    }

    // --- Weekly ---

    fun getWeeklyUsage(): LiveData<List<UsageEntity>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = sdf.format(cal.time)
        return usageDao.getUsageInRangeLive(startDate, endDate)
    }

    suspend fun getWeeklyAppSummary(): List<AppUsageSummary> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = sdf.format(cal.time)
        return usageDao.getAggregatedUsageByApp(startDate, endDate)
    }

    suspend fun getWeeklyDailyTotals(): List<DailyTotal> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = sdf.format(cal.time)
        return usageDao.getDailyTotals(startDate, endDate)
    }

    // --- Monthly ---

    fun getMonthlyUsage(): LiveData<List<UsageEntity>> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val startDate = sdf.format(cal.time)
        return usageDao.getUsageInRangeLive(startDate, endDate)
    }

    suspend fun getMonthlyAppSummary(): List<AppUsageSummary> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val startDate = sdf.format(cal.time)
        return usageDao.getAggregatedUsageByApp(startDate, endDate)
    }

    suspend fun getMonthlyDailyTotals(): List<DailyTotal> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        val endDate = sdf.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -29)
        val startDate = sdf.format(cal.time)
        return usageDao.getDailyTotals(startDate, endDate)
    }

    // --- Cleanup ---

    suspend fun cleanupOldData(daysToKeep: Int = 90) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysToKeep)
        usageDao.deleteOldData(sdf.format(cal.time))
    }
}
