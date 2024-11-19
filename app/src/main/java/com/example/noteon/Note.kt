package com.example.noteon

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    var folderId: Long = 0,
    var isFavorite: Boolean = false,
    var isDeleted: Boolean = false,
    var deletedDate: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    var isSynced: Boolean = false,  // Track if note is synced to Firebase
    var userId: String? = null      // Firebase user ID
)

