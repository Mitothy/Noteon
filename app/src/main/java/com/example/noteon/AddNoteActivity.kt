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

        folderId = intent.getLongExtra("folder_id", 0)

        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        buttonSave = findViewById(R.id.buttonSave)

        if (folderId != 0L) {
            DataHandler.getFolderById(folderId)?.let { folder ->
                supportActionBar?.title = getString(R.string.new_note_in_folder, folder.name)
            }
        }

        buttonSave.setOnClickListener {
            saveNote()
        }
    }

    private fun saveNote() {
        val title = editTextTitle.text.toString().trim()
        val content = editTextContent.text.toString().trim()

        if (title.isNotEmpty() && content.isNotEmpty()) {
            // Use the folder ID when creating the note
            DataHandler.addNote(title, content, folderId)
            finish()
        }
    }
}