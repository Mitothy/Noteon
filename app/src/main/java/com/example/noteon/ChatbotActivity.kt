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

        // Set title based on mode
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
                stackFromEnd = true
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

    private fun sendMessage(message: String) {
        lifecycleScope.launch {
            try {
                // Add user message immediately
                messages.add(ChatMessage(message, true))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)

                // Get response from OpenAI
                val updatedMessages = chatRepository.sendMessage(message)

                // Clear existing messages and add all updated messages
                messages.clear()
                messages.addAll(updatedMessages)
                chatAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messages.size - 1)
            } catch (e: Exception) {
                messages.add(ChatMessage("Sorry, there was an error: ${e.message}", false))
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerView.scrollToPosition(messages.size - 1)
            }
        }
    }

    private suspend fun addInitialMessage() {
        if (noteId != -1L) {
            val note = DataHandler.getNoteById(noteId)
            note?.let {
                val initialMessage = when (chatMode) {
                    ChatMode.CHAT -> {
                        "I'd like to chat about the note titled '${note.title}'. Here's the content:\n\n${note.content}"
                    }
                    ChatMode.SUMMARIZE -> {
                        "Please summarize this note:\n\nTitle: ${note.title}\n\nContent: ${note.content}"
                    }
                    ChatMode.GENERAL -> getString(R.string.chatbot_welcome)
                }
                val responses = chatRepository.sendMessage(initialMessage)
                messages.addAll(responses)
                chatAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messages.size - 1)
            }
        } else {
            val responses = chatRepository.sendMessage(getString(R.string.chatbot_welcome))
            messages.addAll(responses)
            chatAdapter.notifyDataSetChanged()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}