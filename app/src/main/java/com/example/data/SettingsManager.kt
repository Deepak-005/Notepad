package com.example.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("offline_notepad_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_AUTO_SAVE_INTERVAL = "auto_save_interval"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_PIN_LOCK = "pin_lock"
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_LAST_ACTIVE_TIME = "last_active_time"
    }

    var theme: String
        get() = prefs.getString(KEY_THEME, "System") ?: "System"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var fontSize: String
        get() = prefs.getString(KEY_FONT_SIZE, "Medium") ?: "Medium"
        set(value) = prefs.edit().putString(KEY_FONT_SIZE, value).apply()

    var fontFamily: String
        get() = prefs.getString(KEY_FONT_FAMILY, "Sans-serif") ?: "Sans-serif"
        set(value) = prefs.edit().putString(KEY_FONT_FAMILY, value).apply()

    var autoSaveInterval: Int
        get() = prefs.getInt(KEY_AUTO_SAVE_INTERVAL, 5)
        set(value) = prefs.edit().putInt(KEY_AUTO_SAVE_INTERVAL, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var pinLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_PIN_LOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_PIN_LOCK, value).apply()

    var pinCode: String
        get() = prefs.getString(KEY_PIN_CODE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PIN_CODE, value).apply()

    var lastActiveTime: Long
        get() = prefs.getLong(KEY_LAST_ACTIVE_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_ACTIVE_TIME, value).apply()
}
