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
            onAIOptions = { note -> AIOptionsDialog(this).show(note) },
            onNoteOptions = { note ->
                DialogUtils.showNoteOptionsDialog(
                    context = this,
                    note = note,
                    isTrashView = true,
                    onRestoreNote = {
                        DataHandler.restoreNoteFromTrash(it.id)
                        loadTrashNotes()
                    },
                    onDeletePermanently = {
                        DataHandler.deleteNotePermanently(it.id)
                        loadTrashNotes()
                    }
                )
            },
            isTrashView = true
        )
        recyclerViewTrash.apply {
            layoutManager = LinearLayoutManager(this@TrashActivity)
            adapter = trashAdapter
        }
    }

    private fun showRestoreDialog(note: Note) {
        DialogUtils.showConfirmationDialog(
            context = this,
            title = getString(R.string.restore_note),
            message = getString(R.string.restore_note_message),
            positiveButton = getString(R.string.restore),
            onConfirm = {
                DataHandler.restoreNoteFromTrash(note.id)
                loadTrashNotes()
            }
        )
    }

    private fun setupEmptyTrashButton() {
        emptyTrashButton = findViewById(R.id.buttonEmptyTrash)
        emptyTrashButton.setOnClickListener {
            showEmptyTrashDialog()
        }
    }

    private fun showEmptyTrashDialog() {
        DialogUtils.showEmptyTrashDialog(
            context = this,
            onConfirm = {
                DataHandler.emptyTrashWithSync(this)
                loadTrashNotes()
            }
        )
    }

    private fun loadTrashNotes() {
        val trashNotes = DataHandler.getTrashNotes()
        trashAdapter.updateNotes(trashNotes)
    }
}