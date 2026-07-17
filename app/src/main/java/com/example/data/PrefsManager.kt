package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kronos_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_GEMINI_KEY = "custom_gemini_key"
        private const val KEY_GOOGLE_ACCESS_TOKEN = "google_access_token"
        private const val KEY_GOOGLE_USER_EMAIL = "google_user_email"
        private const val KEY_CALENDAR_SYNC_ENABLED = "calendar_sync_enabled"
        private const val KEY_IS_24_HOUR = "is_24_hour"
        private const val KEY_REMINDER_OFFSET = "reminder_offset"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_HAS_SHOWN_API_KEY_ONBOARDING = "has_shown_api_key_onboarding"
        private const val KEY_ACTIVITY_PRE_NOTIFY_HOURS = "activity_pre_notify_hours"
    }

    var activityPreNotifyHours: Int
        get() = prefs.getInt(KEY_ACTIVITY_PRE_NOTIFY_HOURS, 4) // Default is 4 hours before (3 to 5 hours range)
        set(value) = prefs.edit().putInt(KEY_ACTIVITY_PRE_NOTIFY_HOURS, value).apply()

    var hasShownApiKeyOnboarding: Boolean
        get() = prefs.getBoolean(KEY_HAS_SHOWN_API_KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SHOWN_API_KEY_ONBOARDING, value).apply()

    var customGeminiKey: String?
        get() = prefs.getString(KEY_CUSTOM_GEMINI_KEY, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_GEMINI_KEY, value).apply()

    var googleAccessToken: String?
        get() = prefs.getString(KEY_GOOGLE_ACCESS_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_GOOGLE_ACCESS_TOKEN, value).apply()

    var googleUserEmail: String?
        get() = prefs.getString(KEY_GOOGLE_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_GOOGLE_USER_EMAIL, value).apply()

    var isCalendarSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_CALENDAR_SYNC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CALENDAR_SYNC_ENABLED, value).apply()

    var is24HourFormat: Boolean
        get() = prefs.getBoolean(KEY_IS_24_HOUR, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_24_HOUR, value).apply()

    var reminderOffset: Int
        get() = prefs.getInt(KEY_REMINDER_OFFSET, 15)
        set(value) = prefs.edit().putInt(KEY_REMINDER_OFFSET, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
