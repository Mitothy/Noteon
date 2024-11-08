package com.example.noteon

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NoteDetailActivity : AppCompatActivity() {

    private lateinit var textViewTitle: TextView
    private lateinit var textViewContent: TextView
    private lateinit var editTextTitle: EditText
    private lateinit var editTextContent: EditText
    private lateinit var fabEdit: FloatingActionButton
    private var noteId: Long = -1
    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        textViewTitle = findViewById(R.id.textViewTitle)
        textViewContent = findViewById(R.id.textViewContent)
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        fabEdit = findViewById(R.id.fabEdit)

        noteId = intent.getLongExtra("NOTE_ID", -1)
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

        setupEditFab()
    }

    private fun displayNote(note: Note) {
        textViewTitle.text = note.title
        textViewContent.text = note.content
        editTextTitle.setText(note.title)
        editTextContent.setText(note.content)
    }

    private fun setupEditFab() {
        fabEdit.setOnClickListener {
            if (isEditMode) {
                saveChanges()
            } else {
                enableEditMode()
            }
        }
    }

    private fun enableEditMode() {
        isEditMode = true

        // Hide TextViews, show EditTexts
        textViewTitle.visibility = View.GONE
        textViewContent.visibility = View.GONE
        editTextTitle.visibility = View.VISIBLE
        editTextContent.visibility = View.VISIBLE

        // Enable editing
        editTextTitle.isEnabled = true
        editTextContent.isEnabled = true

        // Change FAB icon to save
        fabEdit.setImageResource(android.R.drawable.ic_menu_save)

        // Request focus on title
        editTextTitle.requestFocus()
    }

    private fun saveChanges() {
        val newTitle = editTextTitle.text.toString().trim()
        val newContent = editTextContent.text.toString().trim()

        if (newTitle.isEmpty()) {
            editTextTitle.error = getString(R.string.title_required)
            return
        }

        DataHandler.getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(
                title = newTitle,
                content = newContent
            )
            DataHandler.updateNote(updatedNote)

            // Update the display
            displayNote(updatedNote)

            // Exit edit mode
            disableEditMode()

            Toast.makeText(this, R.string.note_updated, Toast.LENGTH_SHORT).show()
        }
    }

    private fun disableEditMode() {
        isEditMode = false

        // Show TextViews, hide EditTexts
        textViewTitle.visibility = View.VISIBLE
        textViewContent.visibility = View.VISIBLE
        editTextTitle.visibility = View.GONE
        editTextContent.visibility = View.GONE

        // Disable editing
        editTextTitle.isEnabled = false
        editTextContent.isEnabled = false

        // Change FAB icon back to edit
        fabEdit.setImageResource(android.R.drawable.ic_menu_edit)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (isEditMode) {
                    showDiscardChangesDialog()
                } else {
                    finish()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDiscardChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.discard_changes)
            .setMessage(R.string.discard_changes_message)
            .setPositiveButton(R.string.discard) { _, _ ->
                disableEditMode()
                // Restore original content
                DataHandler.getNoteById(noteId)?.let { displayNote(it) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isEditMode) {
            showDiscardChangesDialog()
        } else {
            super.onBackPressed()
        }
    }
}