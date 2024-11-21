package com.example.noteon

import android.content.Context
import android.content.SharedPreferences

class GuestSession private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "guest_session"
        private const val KEY_IS_GUEST = "is_guest"
        private const val KEY_GUEST_ID = "guest_id"

        @Volatile
        private var instance: GuestSession? = null

        fun getInstance(context: Context): GuestSession {
            return instance ?: synchronized(this) {
                instance ?: GuestSession(context.applicationContext).also { instance = it }
            }
        }
    }

    fun startGuestSession() {
        prefs.edit().putBoolean(KEY_IS_GUEST, true).apply()
        // Generate a pseudo-ID for guest users to tag their notes
        if (!prefs.contains(KEY_GUEST_ID)) {
            val guestId = "guest_${System.currentTimeMillis()}"
            prefs.edit().putString(KEY_GUEST_ID, guestId).apply()
        }
    }

    fun endGuestSession() {
        prefs.edit()
            .putBoolean(KEY_IS_GUEST, false)
            .remove(KEY_GUEST_ID)
            .apply()
    }

    fun getGuestId(): String? {
        return if (isGuestSession()) {
            prefs.getString(KEY_GUEST_ID, null)
        } else null
    }

    fun hasGuestData(context: Context): Boolean {
        val guestId = getGuestId()
        return guestId?.let { id ->
            DataHandler.getAllNotes().any { it.userId == id } ||
                    DataHandler.getAllFolders().any { it.userId == id }
        } ?: false
    }

    fun isGuestSession(): Boolean {
        return prefs.getBoolean(KEY_IS_GUEST, false)
    }

    fun clearGuestData(context: Context) {
        // Get the guest ID
        getGuestId()?.let { guestId ->
            // Clear data using DataHandler
            DataHandler.clearUserData(guestId)
        }
        // End the guest session
        endGuestSession()
    }
}