package com.example.noteon

class ChatRepository {
    private val openAIService = OpenAIService.create()
    private var currentThreadId: String? = null
    private var lastMessageTimestamp: Long = 0L

    suspend fun startNewChat(): String {
        val response = openAIService.createThread()
        currentThreadId = response.id
        lastMessageTimestamp = response.created_at
        return response.id
    }

    suspend fun sendMessage(message: String, instructions: String? = null): List<ChatMessage> {
        val threadId = currentThreadId ?: startNewChat()

        if (message.isNotBlank()) {
            openAIService.addMessage(threadId, MessageRequest(content = message))
        }

        val runResponse = openAIService.createRun(
            threadId,
            RunRequest(
                assistant_id = OpenAIConfig.ASSISTANT_ID,
                instructions = instructions
            )
        )
        var run = runResponse

        while (run.status != "completed") {
            if (run.status == "failed" || run.status == "cancelled") {
                throw Exception("Assistant run failed with status: ${run.status}")
            }
            kotlinx.coroutines.delay(1000)
            run = openAIService.getRun(threadId, run.id)
        }

        // Fetch new messages since the last timestamp
        val messagesResponse = openAIService.getMessages(threadId, order = "asc", limit = 100)
        val allMessages = messagesResponse.data

        // Filter new messages since the last timestamp
        val newMessages = allMessages.filter { it.created_at > lastMessageTimestamp }

        // Update the lastMessageTimestamp
        if (allMessages.isNotEmpty()) {
            lastMessageTimestamp = allMessages.last().created_at
        }

        // Filter assistant's messages only
        val assistantMessages = newMessages.filter { it.role == "assistant" }

        // Return ChatMessage objects
        return assistantMessages.map { msg ->
            ChatMessage(
                content = msg.content.firstOrNull()?.text?.value ?: "",
                isUser = false
            )
        }
    }
}