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
    fun show(
        note: Note,
        isTrashView: Boolean = false,
        onUpdate: () -> Unit
    ) {
        if (isTrashView) {
            showTrashOptions(note, onUpdate)
        } else {
            showNormalOptions(note, onUpdate)
        }
    }

    private fun showTrashOptions(note: Note, onUpdate: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.note_options)
            .setItems(arrayOf(
                context.getString(R.string.restore),
                context.getString(R.string.delete_permanently)
            )) { _, which ->
                when (which) {
                    0 -> {
                        DataHandler.restoreNoteFromTrash(note.id)
                        onUpdate()
                    }
                    1 -> showDeletePermanentlyDialog(note, onUpdate)
                }
            }
            .show()
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


    private fun showNormalOptions(note: Note, onUpdate: () -> Unit) {
        val options = arrayOf(
            context.getString(if (note.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites),
            context.getString(R.string.move_to_folder),
            context.getString(R.string.move_to_trash)
        )

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.note_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        DataHandler.toggleNoteFavorite(note.id)
                        onUpdate()
                    }
                    1 -> showMoveToFolderDialog(note, onUpdate)
                    2 -> {
                        DataHandler.moveNoteToTrash(note.id)
                        onUpdate()
                    }
                }
            }
            .show()
    }

    private fun showMoveToFolderDialog(note: Note, onUpdate: () -> Unit) {
        coroutineScope.launch {
            val isSmartCategorizationEnabled = PreferencesManager.getInstance(context).isSmartCategorizationEnabled()
            var loadingDialog: AlertDialog? = null

            try {
                if (isSmartCategorizationEnabled) {
                    // Show loading dialog on the main thread
                    withContext(Dispatchers.Main) {
                        loadingDialog = MaterialAlertDialogBuilder(context)
                            .setTitle(R.string.smart_categorization)
                            .setMessage(R.string.sorting_folders)
                            .setCancelable(false)
                            .show()
                    }
                }

                val folders = DataHandler.getAllFolders().filter { it.id != note.folderId }
                val options = mutableListOf<String>()
                val folderIds = mutableListOf<Long>()

                // Add "Remove from folder" option if note is in a folder
                if (note.folderId != 0L) {
                    options.add(context.getString(R.string.remove_from_folder))
                    folderIds.add(0L)
                }

                val sortedFolders = if (isSmartCategorizationEnabled) {
                    withContext(Dispatchers.IO) {
                        SmartCategorizationService().getSortedFolders(note, folders)
                    }
                } else {
                    folders
                }

                // Add folders to options
                sortedFolders.forEach { folder ->
                    options.add(folder.name)
                    folderIds.add(folder.id)
                }

                // Dismiss loading dialog on the main thread
                if (isSmartCategorizationEnabled) {
                    withContext(Dispatchers.Main) {
                        loadingDialog?.dismiss()
                    }
                }

                // Show folder selection dialog on the main thread
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle(R.string.move_to_folder)
                        .setItems(options.toTypedArray()) { _, which ->
                            val selectedFolderId = folderIds[which]
                            DataHandler.moveNoteToFolder(note.id, selectedFolderId)
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
}