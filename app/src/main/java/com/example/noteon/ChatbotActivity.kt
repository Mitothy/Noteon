package com.example.noteon

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar

class ChatbotActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var toolbar: Toolbar
    private val messages = mutableListOf<ChatMessage>()

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

        // Add initial message based on mode
        addInitialMessage()
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
        // Add user message
        messages.add(ChatMessage(message, true))
        chatAdapter.notifyItemInserted(messages.size - 1)

        // Add chatbot response
        val note = if (noteId != -1L) DataHandler.getNoteById(noteId) else null
        val response = when {
            note != null -> "I understand you're asking about the note '${note.title}': '$message'. This is a placeholder response."
            else -> "I understand you're asking about: '$message'. This is a placeholder response."
        }
        messages.add(ChatMessage(response, false))
        chatAdapter.notifyItemInserted(messages.size - 1)

        // Scroll to bottom and clear input
        recyclerView.scrollToPosition(messages.size - 1)
        editTextMessage.text.clear()
    }

    private fun addInitialMessage() {
        if (noteId != -1L) {
            val note = DataHandler.getNoteById(noteId)
            note?.let {
                val initialMessage = when (chatMode) {
                    ChatMode.CHAT -> getString(R.string.chat_with_note_intro, note.title)
                    ChatMode.SUMMARIZE -> {
                        messages.add(ChatMessage("Please summarize this note: ${note.title}", true))
                        getString(R.string.summarize_content_response, note.title)
                    }
                    ChatMode.GENERAL -> getString(R.string.chatbot_welcome)
                }
                messages.add(ChatMessage(initialMessage, false))
                chatAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messages.size - 1)
            }
        } else {
            messages.add(ChatMessage(getString(R.string.chatbot_welcome), false))
            chatAdapter.notifyDataSetChanged()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}