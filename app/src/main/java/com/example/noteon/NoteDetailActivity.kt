package com.example.noteon

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class NoteDetailActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var textViewTitle: TextView
    private lateinit var textViewContent: TextView
    private lateinit var editTextTitle: EditText
    private lateinit var editTextContent: EditText
    private lateinit var fabEdit: FloatingActionButton
    private lateinit var formattingToolbar: View
    private lateinit var textFormatter: TextFormatter
    private lateinit var formatButtons: List<MaterialButton>
    private var noteId: Long = -1
    private var isEditMode = false
    private var currentNote: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_detail)

        setupViews()
        setupToolbar()
        setupBackPressHandler()
        loadNote()
    }

    private fun setupViews() {
        // Initialize views
        toolbar = findViewById(R.id.toolbar)
        textViewTitle = findViewById(R.id.textViewTitle)
        textViewContent = findViewById(R.id.textViewContent)
        editTextTitle = findViewById(R.id.editTextTitle)
        editTextContent = findViewById(R.id.editTextContent)
        fabEdit = findViewById(R.id.fabEdit)
        formattingToolbar = findViewById(R.id.formattingToolbar)

        textFormatter = TextFormatter(editTextContent)
        setupFormattingButtons()

        fabEdit.setOnClickListener {
            if (isEditMode) {
                saveChanges()
            } else {
                enableEditMode()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            title = getString(R.string.note_detail)
        }
    }

    private fun setupFormattingButtons() {
        formatButtons = listOf(
            findViewById(R.id.buttonBold),
            findViewById(R.id.buttonItalic),
            findViewById(R.id.buttonStrike),
            findViewById(R.id.buttonBullet),
            findViewById(R.id.buttonQuote),
            findViewById(R.id.buttonHeading)
        )

        findViewById<MaterialButton>(R.id.buttonBold).setOnClickListener { textFormatter.toggleBold() }
        findViewById<MaterialButton>(R.id.buttonItalic).setOnClickListener { textFormatter.toggleItalic() }
        findViewById<MaterialButton>(R.id.buttonUnderline).setOnClickListener { textFormatter.toggleUnderline() }
        findViewById<MaterialButton>(R.id.buttonStrike).setOnClickListener { textFormatter.toggleStrikethrough() }
        findViewById<MaterialButton>(R.id.buttonBullet).setOnClickListener { textFormatter.addBulletPoint() }
        findViewById<MaterialButton>(R.id.buttonNumbered).setOnClickListener { textFormatter.addNumberedList() }
        findViewById<MaterialButton>(R.id.buttonQuote).setOnClickListener { textFormatter.addQuote() }
        findViewById<MaterialButton>(R.id.buttonHeading).setOnClickListener { textFormatter.addHeading() }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isEditMode) {
                    showDiscardChangesDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun loadNote() {
        noteId = intent.getLongExtra("NOTE_ID", -1)
        if (noteId != -1L) {
            DataHandler.getNoteById(noteId)?.let { note ->
                currentNote = note
                displayNote(note)
            } ?: finish()
        } else {
            finish()
        }
    }

    private fun displayNote(note: Note) {
        textViewTitle.text = note.title
        MarkdownUtils.render(textViewContent, note.content)
        editTextTitle.setText(note.title)
        editTextContent.setText(note.content)
        supportActionBar?.title = note.title

        // Disable editing for notes in trash
        fabEdit.visibility = if (note.isTrashed()) View.GONE else View.VISIBLE
    }

    private fun enableEditMode() {
        if (currentNote?.isTrashed() == true) return

        isEditMode = true
        textViewTitle.visibility = View.GONE
        textViewContent.visibility = View.GONE
        editTextTitle.visibility = View.VISIBLE
        editTextContent.visibility = View.VISIBLE
        formattingToolbar.visibility = View.VISIBLE

        editTextTitle.isEnabled = true
        editTextContent.isEnabled = true

        fabEdit.setImageResource(android.R.drawable.ic_menu_save)
        editTextTitle.requestFocus()

        invalidateOptionsMenu()
    }

    private fun disableEditMode() {
        isEditMode = false
        textViewTitle.visibility = View.VISIBLE
        textViewContent.visibility = View.VISIBLE
        editTextTitle.visibility = View.GONE
        editTextContent.visibility = View.GONE
        formattingToolbar.visibility = View.GONE

        editTextTitle.isEnabled = false
        editTextContent.isEnabled = false

        fabEdit.setImageResource(android.R.drawable.ic_menu_edit)

        invalidateOptionsMenu()
    }

    private fun saveChanges() {
        val newTitle = editTextTitle.text.toString().trim()
        val newContent = editTextContent.text.toString().trim()

        if (newTitle.isEmpty()) {
            editTextTitle.error = getString(R.string.title_required)
            return
        }

        currentNote?.let { note ->
            val updatedNote = note.copy(
                title = newTitle,
                content = newContent,
                metadata = note.metadata.copy(
                    syncStatus = SyncStatus.NotSynced
                )
            )
            DataHandler.updateNote(updatedNote)
            currentNote = updatedNote
            displayNote(updatedNote)
            disableEditMode()
        }
    }

    private fun showDiscardChangesDialog() {
        DialogUtils.showDiscardChangesDialog(
            context = this,
            onDiscard = {
                disableEditMode()
                currentNote?.let { displayNote(it) }
            }
        )
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
}