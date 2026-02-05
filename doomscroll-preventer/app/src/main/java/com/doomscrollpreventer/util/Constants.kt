package com.doomscrollpreventer.util

object Constants {

    // Default time limits in milliseconds
    const val DEFAULT_WARNING_LIMIT_MS = 15 * 60 * 1000L  // 15 minutes
    const val DEFAULT_BLOCK_LIMIT_MS = 30 * 60 * 1000L    // 30 minutes

    // Scroll detection thresholds
    const val SCROLL_IDLE_TIMEOUT_MS = 60 * 1000L  // 1 minute of no scrolling resets active session
    const val SCROLL_EVENT_BATCH_MS = 1000L         // Batch scroll events within 1 second

    // Target app package names
    val TARGET_APPS = mapOf(
        "com.twitter.android" to "Twitter / X",
        "com.twitter.android.lite" to "Twitter Lite",
        "com.instagram.android" to "Instagram",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.reddit.frontpage" to "Reddit",
        "com.google.android.youtube" to "YouTube",
        "com.facebook.katana" to "Facebook",
        "com.facebook.lite" to "Facebook Lite",
        "com.snapchat.android" to "Snapchat",
        "com.pinterest" to "Pinterest",
        "com.tumblr" to "Tumblr",
        "com.linkedin.android" to "LinkedIn"
    )

    fun getAppDisplayName(packageName: String): String {
        return TARGET_APPS[packageName] ?: packageName.substringAfterLast(".")
            .replaceFirstChar { it.uppercase() }
    }

    // SharedPreferences keys
    const val PREFS_NAME = "doomscroll_prefs"
    const val PREF_ONBOARDING_COMPLETE = "onboarding_complete"
    const val PREF_WARNING_LIMIT = "warning_limit_ms"
    const val PREF_BLOCK_LIMIT = "block_limit_ms"
    const val PREF_MONITORING_ENABLED = "monitoring_enabled"
    const val PREF_ENABLED_APPS = "enabled_apps"
}
