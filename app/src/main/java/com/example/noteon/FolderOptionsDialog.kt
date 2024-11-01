package com.example.noteon

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FolderOptionsDialog(private val context: Context) {
    fun show(folder: Folder, onUpdate: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.folder_options)
            .setItems(R.array.folder_options) { _, which ->
                when (which) {
                    0 -> showEditFolderDialog(folder, onUpdate)
                    1 -> showDeleteFolderConfirmation(folder, onUpdate)
                }
            }
            .show()
    }

    private fun showEditFolderDialog(folder: Folder, onUpdate: () -> Unit) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_create_folder, null)
        val editTextName = view.findViewById<EditText>(R.id.editTextFolderName)
        val editTextDescription = view.findViewById<EditText>(R.id.editTextFolderDescription)

        editTextName.setText(folder.name)
        editTextDescription.setText(folder.description)

        AlertDialog.Builder(context)
            .setTitle(R.string.edit_folder)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = editTextName.text.toString().trim()
                val newDescription = editTextDescription.text.toString().trim()
                if (newName.isNotEmpty()) {
                    DataHandler.updateFolder(folder.id, newName, newDescription)
                    onUpdate()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteFolderConfirmation(folder: Folder, onUpdate: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(R.string.delete_folder)
            .setMessage(R.string.delete_folder_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                DataHandler.deleteFolder(folder.id)
                onUpdate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}