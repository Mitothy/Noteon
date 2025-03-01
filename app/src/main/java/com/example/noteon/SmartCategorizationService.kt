package com.example.noteon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartCategorizationService private constructor() {
    private val openAIService = OpenAIService.create()
    private var currentThreadId: String? = null

    companion object {
        @Volatile
        private var instance: SmartCategorizationService? = null

        fun getInstance(): SmartCategorizationService {
            return instance ?: synchronized(this) {
                instance ?: SmartCategorizationService().also { instance = it }
            }
        }
    }

    suspend fun getSortedFolders(note: Note, folders: List<Folder>): List<Folder> = withContext(Dispatchers.IO) {
        try {
            val threadResponse = openAIService.createThread()
            currentThreadId = threadResponse.id

            val message = buildString {
                append("Note Title: ${note.title}\n")
                append("Note Content: ${note.content}\n\n")
                append("Available Folders:\n")
                folders.forEach { folder ->
                    append("Folder ID: ${folder.id}, Name: ${folder.name}, Description: ${folder.description}\n")
                }
                append("\nPlease return the folder IDs in order of relevance to this note, separated by commas.")
            }

            openAIService.addMessage(currentThreadId!!, MessageRequest(content = message))

            val runResponse = openAIService.createRun(
                currentThreadId!!,
                RunRequest(
                    assistant_id = OpenAIConfig.CLASSIFIER_ASSISTANT_ID
                )
            )

            var run = runResponse
            while (run.status != "completed") {
                if (run.status == "failed" || run.status == "cancelled") {
                    return@withContext folders
                }
                kotlinx.coroutines.delay(1000)
                run = openAIService.getRun(currentThreadId!!, run.id)
            }

            val messagesResponse = openAIService.getMessages(currentThreadId!!, order = "desc", limit = 1)
            val response = messagesResponse.data.firstOrNull()?.content?.firstOrNull()?.text?.value ?: ""

            // Parse the response and maintain order in a List
            val folderIds = response.split(",")
                .mapNotNull { it.trim().toLongOrNull() }

            // Sort folders based on the AI response
            folders.sortedWith(compareBy { folder ->
                val index = folderIds.indexOf(folder.id)
                if (index == -1) Int.MAX_VALUE else index
            })
        } catch (e: Exception) {
            folders
        }
    }
}