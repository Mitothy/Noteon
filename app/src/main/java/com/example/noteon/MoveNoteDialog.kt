package com.example.noteon

import android.content.Context
import androidx.appcompat.app.AlertDialog

class MoveNoteDialog(private val context: Context) {
    fun show(noteId: Long, currentFolderId: Long, onFolderSelected: (Long) -> Unit) {
        val folders = DataHandler.getAllFolders()
        val options = mutableListOf<String>()
        val folderIds = mutableListOf<Long>()

        // Add "Remove from folder" option if note is in a folder
        if (currentFolderId != 0L) {
            options.add(context.getString(R.string.remove_from_folder))
            folderIds.add(0) // 0 represents no folder
        }

        // Add all folders except the current one
        folders.forEach { folder ->
            if (folder.id != currentFolderId) {
                options.add(context.getString(R.string.move_to_folder_name, folder.name))
                folderIds.add(folder.id)
            }
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.move_note)
            .setItems(options.toTypedArray()) { _, which ->
                val selectedFolderId = folderIds[which]
                DataHandler.moveNoteToFolder(noteId, selectedFolderId)
                onFolderSelected(selectedFolderId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}