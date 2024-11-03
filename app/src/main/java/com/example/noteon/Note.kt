package com.example.noteon

data class Note(
    var id: Long = 0,
    var title: String,
    var content: String,
    var folderId: Long = 0,
    var timestamp: Long = System.currentTimeMillis(),
    var isFavorite: Boolean = false
)