package com.example.noteon

import android.content.Context
import androidx.appcompat.app.AlertDialog

class MoveNoteDialog(private val context: Context) {
    fun show(noteId: Long, currentFolderId: Long, onFolderSelected: (Long) -> Unit) {
        val note = DataHandler.getNoteById(noteId) ?: return

        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        // Add favorite/unfavorite option
        if (note.isFavorite) {
            options.add(context.getString(R.string.remove_from_favorites))
            actions.add {
                DataHandler.toggleNoteFavorite(noteId)
                onFolderSelected(currentFolderId)
            }
        } else {
            options.add(context.getString(R.string.add_to_favorites))
            actions.add {
                DataHandler.toggleNoteFavorite(noteId)
                onFolderSelected(currentFolderId)
            }
        }

        // Add move to... option
        options.add(context.getString(R.string.move_to))
        actions.add {
            showMoveToFolderDialog(noteId, currentFolderId, onFolderSelected)
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.note_options)
            .setItems(options.toTypedArray()) { _, which ->
                actions[which].invoke()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showMoveToFolderDialog(noteId: Long, currentFolderId: Long, onFolderSelected: (Long) -> Unit) {
        val options = mutableListOf<String>()
        val folderIds = mutableListOf<Long>()

        // Add "Remove from folder" option if note is in a folder
        if (currentFolderId != 0L) {
            options.add(context.getString(R.string.remove_from_folder))
            folderIds.add(0)
        }

        // Add all folders except the current one
        DataHandler.getAllFolders().forEach { folder ->
            if (folder.id != currentFolderId) {
                options.add(folder.name)
                folderIds.add(folder.id)
            }
        }

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.move_to))
            .setItems(options.toTypedArray()) { _, which ->
                val selectedFolderId = folderIds[which]
                DataHandler.moveNoteToFolder(noteId, selectedFolderId)
                onFolderSelected(selectedFolderId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}