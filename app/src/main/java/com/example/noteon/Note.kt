package com.example.noteon

data class Note(
    var id: Long = 0,
    var title: String,
    var content: String,
    var folderId: Long = 0, // 0 represents root/no folder
    var timestamp: Long = System.currentTimeMillis()
)