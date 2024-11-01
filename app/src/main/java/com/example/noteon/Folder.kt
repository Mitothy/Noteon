package com.example.noteon

data class Folder(
    val id: Long,
    var name: String,
    var description: String,
    var timestamp: Long = System.currentTimeMillis()
)