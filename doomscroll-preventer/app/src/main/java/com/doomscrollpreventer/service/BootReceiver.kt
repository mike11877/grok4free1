package com.doomscrollpreventer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doomscrollpreventer.util.Constants

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(Constants.PREF_MONITORING_ENABLED, true)) {
                // The Accessibility Service will be restarted by the system automatically
                // if it was enabled. This receiver is here for any additional initialization.
            }
        }
    }
}
