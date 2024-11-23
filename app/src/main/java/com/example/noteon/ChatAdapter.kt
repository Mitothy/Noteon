package com.example.noteon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private var messages: List<ChatMessage>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_LOADING = 3
        private const val VIEW_TYPE_NOTE = 4
    }

    private var isLoading = false

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1 && isLoading) {
            VIEW_TYPE_LOADING
        } else {
            when {
                messages[position].isNote -> VIEW_TYPE_NOTE
                messages[position].isUser -> VIEW_TYPE_USER
                else -> VIEW_TYPE_BOT
            }
        }
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)

        fun bind(message: ChatMessage) {
            textViewMessage.text = message.content
        }
    }

    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_loading, parent, false)
                LoadingViewHolder(view)
            }
            VIEW_TYPE_NOTE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_note, parent, false)
                ChatViewHolder(view)
            }
            VIEW_TYPE_USER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_user, parent, false)
                ChatViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chat_bot, parent, false)
                ChatViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ChatViewHolder && position < messages.size) {
            holder.bind(messages[position])
        }
    }

    override fun getItemCount(): Int = messages.size + if (isLoading) 1 else 0

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    fun setLoading(loading: Boolean) {
        if (isLoading != loading) {
            isLoading = loading
            if (loading) {
                notifyItemInserted(itemCount - 1)
            } else {
                notifyItemRemoved(itemCount)
            }
        }
    }
}