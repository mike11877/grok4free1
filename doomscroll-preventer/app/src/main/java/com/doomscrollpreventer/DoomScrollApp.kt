package com.doomscrollpreventer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.doomscrollpreventer.data.AppDatabase

class DoomScrollApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val serviceChannel = NotificationChannel(
            CHANNEL_SERVICE,
            "Scroll Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background monitoring of scroll activity"
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS,
            "Doomscroll Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Warnings and blocks when scroll limits are reached"
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertChannel)
    }

    companion object {
        const val CHANNEL_SERVICE = "scroll_monitor_service"
        const val CHANNEL_ALERTS = "doomscroll_alerts"
    }
}
