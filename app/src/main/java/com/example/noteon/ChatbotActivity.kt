package com.example.noteon

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var toolbar: Toolbar
    private val messages = mutableListOf<ChatMessage>()
    private val chatRepository = ChatRepository()

    private var noteId: Long = -1
    private var noteTitle: String? = null
    private var chatMode: ChatMode = ChatMode.GENERAL

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_NOTE_TITLE = "extra_note_title"
        const val EXTRA_CHAT_MODE = "extra_chat_mode"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1)
        noteTitle = intent.getStringExtra(EXTRA_NOTE_TITLE)
        chatMode = try {
            ChatMode.valueOf(intent.getStringExtra(EXTRA_CHAT_MODE) ?: ChatMode.GENERAL.name)
        } catch (e: IllegalArgumentException) {
            ChatMode.GENERAL
        }

        setupToolbar()
        setupViews()
        setupRecyclerView()
        setupSendButton()

        // Start chat session and add initial message
        lifecycleScope.launch {
            chatRepository.startNewChat()
            addInitialMessage()
        }
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val title = when {
            noteTitle != null -> getString(R.string.chatbot_note_title, noteTitle)
            else -> getString(R.string.chatbot)
        }
        supportActionBar?.title = title
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewChat)
        editTextMessage = findViewById(R.id.editTextMessage)
        buttonSend = findViewById(R.id.buttonSend)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatbotActivity).apply {
                stackFromEnd = false
                reverseLayout = false
            }
            adapter = chatAdapter
        }
    }

    private fun setupSendButton() {
        buttonSend.setOnClickListener {
            val message = editTextMessage.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                editTextMessage.text.clear()
            }
        }
    }

    private fun formatNotesContext(): String {
        val allNotes = DataHandler.getAllNotes()
            .filter { !it.isTrashed() }

        return buildString {
            append("Here are all the user's notes for context:\n\n")
            allNotes.forEachIndexed { index, note ->
                append("Note ${index + 1}:\n")
                append("Title: ${note.title}\n")
                append("Content: ${note.content}\n")
                append("State: ${
                    when {
                        note.isFavorite() -> "Favorite"
                        note.isActive() -> "Active"
                        else -> "Unknown"
                    }
                }\n")
                if (index < allNotes.size - 1) {
                    append("\n---\n\n")
                }
            }
            append("\nPlease use this context to help answer user queries about their notes.")
        }
    }

    private fun sendMessage(message: String) {
        lifecycleScope.launch {
            try {
                // Add user message to local list
                messages.add(ChatMessage(message, true))
                chatAdapter.updateMessages(messages)
                scrollToBottom()

                // Get assistant's response
                chatAdapter.setLoading(true)
                scrollToBottom()
                val assistantResponses = chatRepository.sendMessage(message)
                chatAdapter.setLoading(false)

                // Add assistant's responses to the local list
                messages.addAll(assistantResponses)
                chatAdapter.updateMessages(messages)
                scrollToBottom()
            } catch (e: Exception) {
                chatAdapter.setLoading(false)
                messages.add(ChatMessage("Sorry, there was an error: ${e.message}", false))
                chatAdapter.updateMessages(messages)
                scrollToBottom()
            }
        }
    }

    private suspend fun addInitialMessage() {
        chatAdapter.setLoading(true)

        try {
            val initialInstructions: String? = when (chatMode) {
                ChatMode.CHAT -> {
                    if (noteId != -1L) {
                        val note = DataHandler.getNoteById(noteId)
                        note?.let {
                            if (!note.isTrashed()) {
                                messages.add(ChatMessage("Note: ${note.title}", isUser = true, isNote = true))
                                chatAdapter.updateMessages(messages)
                                "Let's discuss this note. Title: ${note.title}. Content: ${note.content}"
                            } else null
                        }
                    } else null
                }
                ChatMode.SUMMARIZE -> {
                    if (noteId != -1L) {
                        val note = DataHandler.getNoteById(noteId)
                        note?.let {
                            if (!note.isTrashed()) {
                                messages.add(ChatMessage("Summarize note: ${note.title}", isUser = true, isNote = true))
                                chatAdapter.updateMessages(messages)
                                "Please summarize this note. Title: ${note.title}. Content: ${note.content}"
                            } else null
                        }
                    } else null
                }
                ChatMode.GENERAL -> {
                    messages.add(ChatMessage("All notes passed to chatbot", isUser = true, isNote = true))
                    chatAdapter.updateMessages(messages)
                    formatNotesContext()
                }
            }

            // Send the initial message as instructions (not shown to user)
            val assistantResponses = if (initialInstructions != null) {
                chatRepository.sendMessage("", instructions = initialInstructions)
            } else {
                chatAdapter.setLoading(false)
                return
            }
            chatAdapter.setLoading(false)

            // Add assistant's responses to the local list
            messages.addAll(assistantResponses)
            chatAdapter.updateMessages(messages)
            scrollToBottom()
        } catch (e: Exception) {
            chatAdapter.setLoading(false)
            messages.add(ChatMessage("Sorry, there was an error initializing the chat: ${e.message}", isUser = false))
            chatAdapter.updateMessages(messages)
            scrollToBottom()
        }
    }

    private fun scrollToBottom() {
        recyclerView.post {
            recyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}