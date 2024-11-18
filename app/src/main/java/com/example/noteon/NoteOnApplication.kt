package com.example.noteon

import android.app.Application

class NoteOnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DataHandler.initialize(this)
    }
}