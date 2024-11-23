package com.example.noteon

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val isNote: Boolean = false
)