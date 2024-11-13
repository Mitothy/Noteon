package com.example.noteon

import android.content.Context
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AIOptionsDialog(private val context: Context) {
    fun show(note: Note) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.ai_options)
            .setItems(arrayOf(
                context.getString(R.string.chat_with_note),
                context.getString(R.string.summarize_content)
            )) { _, which ->
                when (which) {
                    0 -> openChatWithNote(note, ChatMode.CHAT)
                    1 -> openChatWithNote(note, ChatMode.SUMMARIZE)
                }
            }
            .show()
    }

    private fun openChatWithNote(note: Note, mode: ChatMode) {
        val intent = Intent(context, ChatbotActivity::class.java).apply {
            putExtra(ChatbotActivity.EXTRA_NOTE_ID, note.id)
            putExtra(ChatbotActivity.EXTRA_NOTE_TITLE, note.title)
            putExtra(ChatbotActivity.EXTRA_CHAT_MODE, mode.name)
        }
        context.startActivity(intent)
    }
}