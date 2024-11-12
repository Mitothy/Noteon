package com.example.noteon

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar

class ChatbotActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var toolbar: Toolbar
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        setupToolbar()
        setupViews()
        setupRecyclerView()
        setupSendButton()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.chatbot)
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
                stackFromEnd = true // Messages start from bottom
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

        // Add chatbot response (placeholder)
        val response = "I understand you're asking about: '$message'. This is a placeholder response."
        messages.add(ChatMessage(response, false))
        chatAdapter.notifyItemInserted(messages.size - 1)

        // Scroll to bottom
        recyclerView.scrollToPosition(messages.size - 1)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean
)

class ChatAdapter(private val messages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)

        fun bind(message: ChatMessage) {
            textViewMessage.text = message.content
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutRes = if (viewType == VIEW_TYPE_USER) {
            R.layout.item_chat_user
        } else {
            R.layout.item_chat_bot
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(layoutRes, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size
}