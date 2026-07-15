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
    }

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
