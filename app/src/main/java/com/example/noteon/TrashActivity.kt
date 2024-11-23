package com.example.noteon

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class TrashActivity : AppCompatActivity() {
    private lateinit var recyclerViewTrash: RecyclerView
    private lateinit var trashAdapter: NotesAdapter
    private lateinit var emptyTrashButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        setupToolbar()
        setupRecyclerView()
        setupEmptyTrashButton()
        loadTrashNotes()
    }

    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.trash)
        }
    }

    private fun setupRecyclerView() {
        recyclerViewTrash = findViewById(R.id.recyclerViewTrash)
        trashAdapter = NotesAdapter(
            notes = emptyList(),
            onNoteClick = { note -> showRestoreDialog(note) },
            coroutineScope = lifecycleScope,
            onAIOptions = { note -> AIOptionsDialog(this).show(note) }
        )
        recyclerViewTrash.apply {
            layoutManager = LinearLayoutManager(this@TrashActivity)
            adapter = trashAdapter
        }
    }

    private fun showRestoreDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle(R.string.restore_note)
            .setMessage(R.string.restore_note_message)
            .setPositiveButton(R.string.restore) { _, _ ->
                DataHandler.restoreNoteFromTrash(note.id)
                loadTrashNotes()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupEmptyTrashButton() {
        emptyTrashButton = findViewById(R.id.buttonEmptyTrash)
        emptyTrashButton.setOnClickListener {
            showEmptyTrashDialog()
        }
    }

    private fun showEmptyTrashDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.empty_trash)
            .setMessage(R.string.empty_trash_message)
            .setPositiveButton(R.string.empty_trash) { _, _ ->
                DataHandler.emptyTrashWithSync(this)
                loadTrashNotes()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadTrashNotes() {
        val trashNotes = DataHandler.getTrashNotes()
        trashAdapter.updateNotes(trashNotes)
    }
}