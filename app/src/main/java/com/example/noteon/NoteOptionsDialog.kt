package com.example.noteon

import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteOptionsDialog(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    fun show(note: Note, onUpdate: () -> Unit) {
        // Get available options based on note state
        val options = note.state.getAvailableOptions()

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.note_options)
            .setItems(
                options.map { context.getString(it.getResourceString()) }.toTypedArray()
            ) { _, which ->
                handleOptionSelection(options[which], note, onUpdate)
            }
            .show()
    }

    private fun handleOptionSelection(
        option: NoteOption,
        note: Note,
        onUpdate: () -> Unit
    ) {
        when (option) {
            is NoteOption.ToggleFavorite -> {
                val updatedNote = if (option.currentlyFavorited) {
                    note.unfavorite()
                } else {
                    note.favorite()
                }
                DataHandler.updateNote(updatedNote)
                onUpdate()
            }

            is NoteOption.MoveToFolder -> {
                showMoveToFolderDialog(note, onUpdate)
            }

            is NoteOption.MoveToTrash -> {
                DataHandler.moveNoteToTrash(note.id)
                onUpdate()
            }

            is NoteOption.Restore -> {
                DataHandler.restoreNoteFromTrash(note.id)
                onUpdate()
            }

            is NoteOption.DeletePermanently -> {
                showDeletePermanentlyDialog(note, onUpdate)
            }

            is NoteOption.AIOptions -> {
                AIOptionsDialog(context).show(note)
            }
        }
    }

    private fun showMoveToFolderDialog(note: Note, onUpdate: () -> Unit) {
        coroutineScope.launch {
            val isSmartCategorizationEnabled = PreferencesManager.getInstance(context)
                .isSmartCategorizationEnabled()
            var loadingDialog: AlertDialog? = null

            try {
                if (isSmartCategorizationEnabled) {
                    withContext(Dispatchers.Main) {
                        loadingDialog = MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.smart_categorization)
                            .setMessage(R.string.sorting_folders)
                            .setCancelable(false)
                            .show()
                    }
                }

                val folders = DataHandler.getAllFolders()
                    .filter { it.id != note.metadata.folderId }
                val options = mutableListOf<String>()
                val folderIds = mutableListOf<Long>()

                // Add "Remove from folder" option if note is in a folder
                if (note.metadata.folderId != 0L) {
                    options.add(context.getString(R.string.remove_from_folder))
                    folderIds.add(0L)
                }

                // Get sorted folders if smart categorization is enabled
                val sortedFolders = if (isSmartCategorizationEnabled) {
                    withContext(Dispatchers.IO) {
                        SmartCategorizationService.getInstance().getSortedFolders(note, folders)
                    }
                } else {
                    folders
                }

                sortedFolders.forEach { folder ->
                    options.add(folder.name)
                    folderIds.add(folder.id)
                }

                loadingDialog?.dismiss()

                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.move_to_folder)
                        .setItems(options.toTypedArray()) { _, which ->
                            DataHandler.moveNoteToFolder(note.id, folderIds[which])
                            onUpdate()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog?.dismiss()
                    Toast.makeText(context, R.string.error_sorting_folders, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDeletePermanentlyDialog(note: Note, onUpdate: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_permanently)
            .setMessage(R.string.delete_permanently_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                DataHandler.deleteNoteWithSync(note.id, context)
                onUpdate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}