package com.example.noteon

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class AddNoteActivity : AppCompatActivity() {
    private lateinit var editTextTitle: EditText
    private lateinit var editTextContent: EditText
    private lateinit var buttonSave: Button
    private var folderId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)

        // Get folder ID if note is being created in a folder
        folderId = intent.getLongExtra("folder_id", 0)

        setupViews()
        setupToolbar()
    }

    private fun setupViews() {
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        buttonSave = findViewById(R.id.buttonSave)

        buttonSave.setOnClickListener {
            saveNote()
        }
    }

    private fun setupToolbar() {
        if (folderId != 0L) {
            DataHandler.getFolderById(folderId)?.let { folder ->
                supportActionBar?.title = getString(R.string.new_note_in_folder, folder.name)
            }
        }
    }

    private fun saveNote() {
        val title = editTextTitle.text.toString().trim()
        val content = editTextContent.text.toString().trim()

        if (validateNote(title, content)) {
            // Create note with provided content and default state (Active)
            DataHandler.addNote(this, title, content, folderId)
            finish()
        }
    }

    private fun validateNote(title: String, content: String): Boolean {
        if (title.isEmpty()) {
            editTextTitle.error = getString(R.string.title_required)
            return false
        }
        if (content.isEmpty()) {
            editTextContent.error = getString(R.string.content_required)
            return false
        }
        return true
    }
}