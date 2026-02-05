package com.doomscrollpreventer.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import com.doomscrollpreventer.DoomScrollApp
import com.doomscrollpreventer.data.UsageEntity
import com.doomscrollpreventer.util.Constants
import com.doomscrollpreventer.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class ScrollDetectorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var prefs: SharedPreferences

    // Tracking state per app
    private val appScrollSessions = mutableMapOf<String, AppScrollState>()
    private var currentForegroundApp: String? = null

    data class AppScrollState(
        var totalScrollTimeMs: Long = 0L,
        var sessionStartTime: Long = 0L,
        var lastScrollEventTime: Long = 0L,
        var isActivelyScrolling: Boolean = false,
        var warningShown: Boolean = false,
        var blocked: Boolean = false,
        val dateKey: String = TimeUtils.todayDateString()
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE)

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_SCROLLED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }

        loadTodayUsage()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!prefs.getBoolean(Constants.PREF_MONITORING_ENABLED, true)) return

        val packageName = event.packageName?.toString() ?: return

        // Check if this is a target app
        if (!isTargetApp(packageName)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleAppSwitch(packageName)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleScrollEvent(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Content changes during scrolling can indicate infinite scroll loading
                if (currentForegroundApp == packageName) {
                    val state = getOrCreateState(packageName)
                    if (state.isActivelyScrolling) {
                        handleScrollEvent(packageName)
                    }
                }
            }
        }
    }

    private fun handleAppSwitch(packageName: String) {
        // Pause tracking for previous app
        currentForegroundApp?.let { prevApp ->
            val prevState = appScrollSessions[prevApp]
            if (prevState?.isActivelyScrolling == true) {
                pauseScrollSession(prevApp, prevState)
            }
        }
        currentForegroundApp = packageName
    }

    private fun handleScrollEvent(packageName: String) {
        val now = System.currentTimeMillis()
        val state = getOrCreateState(packageName)

        // Reset if day changed
        if (state.dateKey != TimeUtils.todayDateString()) {
            saveUsageToDb(packageName, state)
            appScrollSessions[packageName] = AppScrollState()
            return
        }

        if (!state.isActivelyScrolling) {
            // Start new scroll session
            state.isActivelyScrolling = true
            state.sessionStartTime = now
            state.lastScrollEventTime = now
        } else {
            val timeSinceLastScroll = now - state.lastScrollEventTime

            if (timeSinceLastScroll > Constants.SCROLL_IDLE_TIMEOUT_MS) {
                // User was idle, save the previous session and start fresh
                val sessionDuration = state.lastScrollEventTime - state.sessionStartTime
                state.totalScrollTimeMs += sessionDuration
                saveUsageToDb(packageName, state)
                state.sessionStartTime = now
            }

            state.lastScrollEventTime = now
        }

        // Calculate current total including active session
        val activeSessionTime = now - state.sessionStartTime
        val totalTime = state.totalScrollTimeMs + activeSessionTime

        checkLimits(packageName, totalTime, state)
    }

    private fun checkLimits(packageName: String, totalTimeMs: Long, state: AppScrollState) {
        val warningLimit = prefs.getLong(Constants.PREF_WARNING_LIMIT, Constants.DEFAULT_WARNING_LIMIT_MS)
        val blockLimit = prefs.getLong(Constants.PREF_BLOCK_LIMIT, Constants.DEFAULT_BLOCK_LIMIT_MS)

        when {
            totalTimeMs >= blockLimit && !state.blocked -> {
                state.blocked = true
                showBlockOverlay(packageName, totalTimeMs)
            }
            totalTimeMs >= warningLimit && !state.warningShown -> {
                state.warningShown = true
                showWarningOverlay(packageName, totalTimeMs)
            }
        }
    }

    private fun showWarningOverlay(packageName: String, totalTimeMs: Long) {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_TYPE, OverlayService.TYPE_WARNING)
            putExtra(OverlayService.EXTRA_APP_NAME, Constants.getAppDisplayName(packageName))
            putExtra(OverlayService.EXTRA_TIME_SPENT, totalTimeMs)
        }
        startForegroundService(intent)
    }

    private fun showBlockOverlay(packageName: String, totalTimeMs: Long) {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_TYPE, OverlayService.TYPE_BLOCK)
            putExtra(OverlayService.EXTRA_APP_NAME, Constants.getAppDisplayName(packageName))
            putExtra(OverlayService.EXTRA_TIME_SPENT, totalTimeMs)
        }
        startForegroundService(intent)
    }

    private fun pauseScrollSession(packageName: String, state: AppScrollState) {
        if (state.isActivelyScrolling) {
            val sessionDuration = state.lastScrollEventTime - state.sessionStartTime
            state.totalScrollTimeMs += sessionDuration
            state.isActivelyScrolling = false
            saveUsageToDb(packageName, state)
        }
    }

    private fun saveUsageToDb(packageName: String, state: AppScrollState) {
        val app = (application as DoomScrollApp)
        serviceScope.launch(Dispatchers.IO) {
            val dateKey = TimeUtils.todayDateString()
            val existing = app.database.usageDao().getUsageForAppOnDate(packageName, dateKey)
            if (existing != null) {
                app.database.usageDao().update(
                    existing.copy(
                        totalScrollTimeMs = state.totalScrollTimeMs,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            } else {
                app.database.usageDao().insert(
                    UsageEntity(
                        packageName = packageName,
                        appDisplayName = Constants.getAppDisplayName(packageName),
                        dateKey = dateKey,
                        totalScrollTimeMs = state.totalScrollTimeMs,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun loadTodayUsage() {
        val app = (application as DoomScrollApp)
        serviceScope.launch(Dispatchers.IO) {
            val today = TimeUtils.todayDateString()
            val todayUsage = app.database.usageDao().getUsageForDate(today)
            todayUsage.forEach { entity ->
                appScrollSessions[entity.packageName] = AppScrollState(
                    totalScrollTimeMs = entity.totalScrollTimeMs,
                    dateKey = today
                )
            }
        }
    }

    private fun getOrCreateState(packageName: String): AppScrollState {
        return appScrollSessions.getOrPut(packageName) { AppScrollState() }
    }

    private fun isTargetApp(packageName: String): Boolean {
        val enabledApps = prefs.getStringSet(Constants.PREF_ENABLED_APPS, null)
        return if (enabledApps != null) {
            packageName in enabledApps
        } else {
            packageName in Constants.TARGET_APPS.keys
        }
    }

    override fun onInterrupt() {
        // Save all current sessions
        appScrollSessions.forEach { (packageName, state) ->
            if (state.isActivelyScrolling) {
                pauseScrollSession(packageName, state)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
