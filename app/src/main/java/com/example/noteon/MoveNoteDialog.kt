package com.example.noteon

import android.content.Context
import androidx.appcompat.app.AlertDialog

class MoveNoteDialog(private val context: Context) {
    fun show(noteId: Long, onFolderSelected: (Long) -> Unit) {
        val folders = DataHandler.getAllFolders()
        val folderNames = folders.map { it.name }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(R.string.move_to_folder)
            .setItems(folderNames) { _, which ->
                val selectedFolderId = folders[which].id
                DataHandler.moveNoteToFolder(noteId, selectedFolderId)
                onFolderSelected(selectedFolderId)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}