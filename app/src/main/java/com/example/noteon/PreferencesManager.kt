package com.example.noteon

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class PreferencesManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    companion object {
        private const val TAG = "PreferencesManager"
        private const val PREF_NAME = "noteon_preferences"
        private const val KEY_SMART_CATEGORIZATION = "smart_categorization"
        private const val KEY_INTELLIGENT_SEARCH = "intelligent_search"

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

    fun setIntelligentSearchEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTELLIGENT_SEARCH, enabled).apply()
    }

    fun isIntelligentSearchEnabled(): Boolean {
        return prefs.getBoolean(KEY_INTELLIGENT_SEARCH, false)
    }

    fun syncSettingsToFirebase(userId: String) {
        val settings = UserSettings(
            intelligentSearchEnabled = isIntelligentSearchEnabled(),
            smartCategorizationEnabled = isSmartCategorizationEnabled()
        )

        database.child("users")
            .child(userId)
            .child("settings")
            .setValue(settings)
            .addOnSuccessListener {
                Log.d(TAG, "Settings synced successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error syncing settings", e)
            }
    }

    suspend fun restoreSettingsFromFirebase(userId: String) {
        try {
            val snapshot = database
                .child("users")
                .child(userId)
                .child("settings")
                .get()
                .await()

            val settings = snapshot.getValue(UserSettings::class.java)
            if (settings != null) {
                setIntelligentSearchEnabled(settings.intelligentSearchEnabled)
                setSmartCategorizationEnabled(settings.smartCategorizationEnabled)
                Log.d(TAG, "Settings restored successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring settings", e)
            throw e
        }
    }

    fun clearSettings() {
        prefs.edit().clear().apply()
    }
}