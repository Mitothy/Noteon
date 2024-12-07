package com.example.noteon

data class UserSettings(
    val intelligentSearchEnabled: Boolean = false,
    val smartCategorizationEnabled: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)