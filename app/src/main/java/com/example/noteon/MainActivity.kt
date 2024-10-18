package com.example.noteon

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var notes: List<Note>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)
        fabAddNote = findViewById(R.id.fabAddNote)

        // Generate dummy data
        notes = DataHandler.generateDummyNotes(20) // Generate 20 dummy notes

        setupRecyclerView()
        setupFab()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(notes) { note ->
            openNoteDetail(note.id)
        }
        recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        recyclerViewNotes.adapter = notesAdapter
    }

    private fun setupFab() {
        fabAddNote.setOnClickListener {
            // Handle add note action
            // For now, we'll just print a message
            println("Add note clicked")
        }
    }

    private fun openNoteDetail(noteId: Long) {
        val intent = Intent(this, NoteDetailActivity::class.java)
        intent.putExtra("NOTE_ID", noteId)
        startActivity(intent)
    }
}