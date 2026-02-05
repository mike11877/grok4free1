package com.doomscrollpreventer.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.doomscrollpreventer.DoomScrollApp
import com.doomscrollpreventer.data.AppUsageSummary
import com.doomscrollpreventer.data.DailyTotal
import com.doomscrollpreventer.data.UsageRepository
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UsageRepository

    // Current time period tab: 0 = Daily, 1 = Weekly, 2 = Monthly
    private val _selectedPeriod = MutableLiveData(0)
    val selectedPeriod: LiveData<Int> = _selectedPeriod

    // Today's total
    val todayTotal: LiveData<Long>

    // Per-app breakdown (switches based on selected period)
    private val _appBreakdown = MutableLiveData<List<AppUsageSummary>>(emptyList())
    val appBreakdown: LiveData<List<AppUsageSummary>> = _appBreakdown

    // Daily totals for chart
    private val _dailyTotals = MutableLiveData<List<DailyTotal>>(emptyList())
    val dailyTotals: LiveData<List<DailyTotal>> = _dailyTotals

    // Total for selected period
    private val _periodTotal = MutableLiveData(0L)
    val periodTotal: LiveData<Long> = _periodTotal

    init {
        val db = (application as DoomScrollApp).database
        repository = UsageRepository(db.usageDao())
        todayTotal = repository.getTodayTotal()
        loadDataForPeriod(0)
    }

    fun selectPeriod(period: Int) {
        _selectedPeriod.value = period
        loadDataForPeriod(period)
    }

    private fun loadDataForPeriod(period: Int) {
        viewModelScope.launch {
            when (period) {
                0 -> { // Daily
                    val apps = repository.run {
                        val dao = (getApplication<DoomScrollApp>()).database.usageDao()
                        val today = com.doomscrollpreventer.util.TimeUtils.todayDateString()
                        dao.getAggregatedUsageByApp(today, today)
                    }
                    _appBreakdown.value = apps
                    _periodTotal.value = apps.sumOf { it.totalScrollTimeMs }
                    _dailyTotals.value = emptyList()
                }
                1 -> { // Weekly
                    val apps = repository.getWeeklyAppSummary()
                    val dailies = repository.getWeeklyDailyTotals()
                    _appBreakdown.value = apps
                    _dailyTotals.value = dailies
                    _periodTotal.value = apps.sumOf { it.totalScrollTimeMs }
                }
                2 -> { // Monthly
                    val apps = repository.getMonthlyAppSummary()
                    val dailies = repository.getMonthlyDailyTotals()
                    _appBreakdown.value = apps
                    _dailyTotals.value = dailies
                    _periodTotal.value = apps.sumOf { it.totalScrollTimeMs }
                }
            }
        }
    }

    fun refresh() {
        loadDataForPeriod(_selectedPeriod.value ?: 0)
    }
}
