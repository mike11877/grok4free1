package com.doomscrollpreventer.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.doomscrollpreventer.DoomScrollApp
import com.doomscrollpreventer.R
import com.doomscrollpreventer.util.TimeUtils

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start as foreground service
        val notification = NotificationCompat.Builder(this, DoomScrollApp.CHANNEL_SERVICE)
            .setContentTitle("DoomScroll Preventer")
            .setContentText("Monitoring your scroll activity")
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        val type = intent?.getStringExtra(EXTRA_TYPE) ?: return START_NOT_STICKY
        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "App"
        val timeSpent = intent.getLongExtra(EXTRA_TIME_SPENT, 0L)

        removeOverlay()

        when (type) {
            TYPE_WARNING -> showWarningOverlay(appName, timeSpent)
            TYPE_BLOCK -> showBlockOverlay(appName, timeSpent)
        }

        return START_NOT_STICKY
    }

    private fun showWarningOverlay(appName: String, timeSpent: Long) {
        val layoutParams = createOverlayLayoutParams(dimBackground = false)
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_warning, null)

        view.findViewById<TextView>(R.id.tvWarningTitle).text = "Time Check"
        view.findViewById<TextView>(R.id.tvWarningMessage).text =
            "You've been scrolling $appName for ${TimeUtils.formatDuration(timeSpent)}.\n\nConsider taking a break."
        view.findViewById<TextView>(R.id.tvTimeSpent).text = TimeUtils.formatDuration(timeSpent)

        view.findViewById<Button>(R.id.btnDismissWarning).setOnClickListener {
            removeOverlay()
            stopSelf()
        }

        overlayView = view
        windowManager?.addView(view, layoutParams)
    }

    private fun showBlockOverlay(appName: String, timeSpent: Long) {
        val layoutParams = createOverlayLayoutParams(dimBackground = true)
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_block, null)

        view.findViewById<TextView>(R.id.tvBlockTitle).text = "Time's Up"
        view.findViewById<TextView>(R.id.tvBlockMessage).text =
            "You've spent ${TimeUtils.formatDuration(timeSpent)} scrolling $appName today.\n\nIt's time to do something else."
        view.findViewById<TextView>(R.id.tvTimeSpent).text = TimeUtils.formatDuration(timeSpent)

        view.findViewById<Button>(R.id.btnCloseApp).setOnClickListener {
            removeOverlay()
            // Send user to home screen
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(homeIntent)
            stopSelf()
        }

        overlayView = view
        windowManager?.addView(view, layoutParams)
    }

    private fun createOverlayLayoutParams(dimBackground: Boolean): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            if (dimBackground) {
                WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            } else {
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            },
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            if (dimBackground) dimAmount = 0.85f
        }
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        overlayView = null
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_TYPE = "overlay_type"
        const val EXTRA_APP_NAME = "app_name"
        const val EXTRA_TIME_SPENT = "time_spent"
        const val TYPE_WARNING = "warning"
        const val TYPE_BLOCK = "block"
        const val NOTIFICATION_ID = 1001
    }
}
