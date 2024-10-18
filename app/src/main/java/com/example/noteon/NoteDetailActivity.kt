package com.example.noteon

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.noteon.R

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var textViewTitle: TextView
    private lateinit var textViewContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        textViewTitle = findViewById(R.id.textViewTitle)
        textViewContent = findViewById(R.id.textViewContent)

        val noteId = intent.getLongExtra("NOTE_ID", -1)
        if (noteId != -1L) {
            val note = DataHandler.getNoteById(noteId)
            if (note != null) {
                displayNote(note)
            } else {
                finish() // Close the activity if note is not found
            }
        } else {
            finish() // Close the activity if no note ID was provided
        }
    }

    private fun displayNote(note: Note) {
        textViewTitle.text = note.title
        textViewContent.text = note.content
    }
}