package com.example.noteon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class IntelligentSearchService private constructor() {
    private val openAIService = OpenAIService.create()
    private var currentThreadId: String? = null

    companion object {
        @Volatile
        private var instance: IntelligentSearchService? = null

        fun getInstance(): IntelligentSearchService {
            return instance ?: synchronized(this) {
                instance ?: IntelligentSearchService().also { instance = it }
            }
        }
    }

    suspend fun searchNotes(query: String, notes: List<Note>): List<Note> = withContext(Dispatchers.IO) {
        try {
            val threadResponse = openAIService.createThread()
            currentThreadId = threadResponse.id

            val message = buildString {
                append("Search Query: $query\n\n")
                append("Available Notes:\n")
                notes.forEachIndexed { index, note ->
                    append("Note ${index + 1}:\n")
                    append("ID: ${note.id}\n")
                    append("Title: ${note.title}\n")
                    append("Content: ${note.content}\n")
                    append("---\n")
                }
                append("\nPlease analyze the search query and return the Note IDs (comma-separated) that are most relevant to this search, considering semantic meaning and context. Include notes that might not contain the exact search terms but are conceptually related.")
            }

            openAIService.addMessage(currentThreadId!!, MessageRequest(content = message))

            val runResponse = openAIService.createRun(
                currentThreadId!!,
                RunRequest(
                    assistant_id = OpenAIConfig.SEARCHER_ASSISTANT_ID
                )
            )

            var run = runResponse
            while (run.status != "completed") {
                if (run.status == "failed" || run.status == "cancelled") {
                    return@withContext performFallbackSearch(query, notes)
                }
                kotlinx.coroutines.delay(1000)
                run = openAIService.getRun(currentThreadId!!, run.id)
            }

            val messagesResponse = openAIService.getMessages(currentThreadId!!, order = "desc", limit = 1)
            val response = messagesResponse.data.firstOrNull()?.content?.firstOrNull()?.text?.value ?: ""

            // Parse the response and sort notes
            val noteIds = response.split(",")
                .mapNotNull { it.trim().toLongOrNull() }
                .toSet()

            // Only return notes that were explicitly mentioned by the AI
            val matchedNotes = notes.filter { note ->
                noteIds.contains(note.id)
            }.sortedWith(compareBy { note ->
                noteIds.indexOf(note.id)
            })

            matchedNotes
        } catch (e: Exception) {
            performFallbackSearch(query, notes)
        }
    }

    private fun performFallbackSearch(query: String, notes: List<Note>): List<Note> {
        val searchTerms = query.lowercase().split(" ")
        return notes.filter { note ->
            val noteText = "${note.title} ${note.content}".lowercase()
            searchTerms.any { term -> noteText.contains(term) }
        }
    }
}