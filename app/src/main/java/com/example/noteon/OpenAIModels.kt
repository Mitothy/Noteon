package com.example.noteon

// Models for API Requests
data class MessageRequest(
    val role: String = "user",
    val content: String
)

data class RunRequest(
    val assistant_id: String,
    val instructions: String? = null
)

// Models for API Responses
data class ThreadResponse(
    val id: String,
    val created_at: Long,
    val status: String? = null
)

data class MessageResponse(
    val id: String,
    val content: List<MessageContent>,
    val role: String,
    val created_at: Long,
    val thread_id: String? = null,
    val assistant_id: String? = null
)

data class MessageContent(
    val type: String,
    val text: TextContent
)

data class TextContent(
    val value: String,
    val annotations: List<Any>? = null
)

data class RunResponse(
    val id: String,
    val status: String,
    val created_at: Long,
    val thread_id: String? = null,
    val assistant_id: String? = null
)

data class MessagesResponse(
    val data: List<MessageResponse>,
    val first_id: String? = null,
    val last_id: String? = null,
    val has_more: Boolean = false
)