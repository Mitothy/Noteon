package com.example.noteon

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "noteon_preferences"
        private const val KEY_SMART_CATEGORIZATION = "smart_categorization"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun setSmartCategorizationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_CATEGORIZATION, enabled).apply()
    }

    fun isSmartCategorizationEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_CATEGORIZATION, false)
    }
}