package com.example.noteon

class ChatRepository {
    private val openAIService = OpenAIService.create()
    private var currentThreadId: String? = null

    suspend fun startNewChat(): String {
        val response = openAIService.createThread()
        currentThreadId = response.id
        return response.id
    }

    suspend fun sendMessage(message: String): List<ChatMessage> {
        val threadId = currentThreadId ?: startNewChat()

        // Send the user's message
        openAIService.addMessage(threadId, MessageRequest(content = message))

        // Create and wait for the run to complete
        val runResponse = openAIService.createRun(threadId, RunRequest(OpenAIConfig.ASSISTANT_ID))
        var run = runResponse

        while (run.status != "completed") {
            if (run.status == "failed" || run.status == "cancelled") {
                throw Exception("Assistant run failed with status: ${run.status}")
            }
            kotlinx.coroutines.delay(1000) // Wait 1 second before checking again
            run = openAIService.getRun(threadId, run.id)
        }

        // Get the latest messages
        val messages = openAIService.getMessages(threadId)
        return messages.data.map { msg ->
            ChatMessage(
                content = msg.content.firstOrNull()?.text?.value ?: "",
                isUser = msg.role == "user"
            )
        }
    }
}