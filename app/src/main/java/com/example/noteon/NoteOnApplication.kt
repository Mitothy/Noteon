package com.example.noteon

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class NoteOnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Enable Firebase persistence before anything else
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        // Initialize DataHandler
        DataHandler.initialize(this)
    }
}