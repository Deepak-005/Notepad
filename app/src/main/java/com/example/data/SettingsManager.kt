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
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGGED_IN_PHONE = "logged_in_phone"
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

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var loggedInPhone: String
        get() = prefs.getString(KEY_LOGGED_IN_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOGGED_IN_PHONE, value).apply()

    // Active Profile properties
    var activeProfileId: String
        get() = prefs.getString("active_profile_id", "personal") ?: "personal"
        set(value) = prefs.edit().putString("active_profile_id", value).apply()

    fun getProfileName(id: String, default: String): String {
        return prefs.getString("profile_name_$id", default) ?: default
    }

    fun setProfileName(id: String, name: String) {
        prefs.edit().putString("profile_name_$id", name).apply()
    }

    fun getProfileEmoji(id: String, default: String): String {
        return prefs.getString("profile_emoji_$id", default) ?: default
    }

    fun setProfileEmoji(id: String, emoji: String) {
        prefs.edit().putString("profile_emoji_$id", emoji).apply()
    }

    fun getProfileColor(id: String, default: String): String {
        return prefs.getString("profile_color_$id", default) ?: default
    }

    fun setProfileColor(id: String, colorHex: String) {
        prefs.edit().putString("profile_color_$id", colorHex).apply()
    }
}
