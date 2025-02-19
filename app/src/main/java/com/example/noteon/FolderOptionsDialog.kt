package com.example.noteon

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class FolderOptionsDialog(private val context: Context) {
    fun show(folder: Folder, onUpdate: () -> Unit) {
        val options = mutableListOf(
            context.getString(R.string.edit_folder),
            context.getString(R.string.delete_folder)
        )

        // Add sync-related options based on current sync status
        when (folder.metadata.syncStatus) {
            is SyncStatus.NotSynced ->
                options.add(context.getString(R.string.sync_folder))
            is SyncStatus.SyncError ->
                options.add(context.getString(R.string.retry_sync))
            is SyncStatus.Synced -> {}
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.folder_options)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> showEditFolderDialog(folder, onUpdate)
                    1 -> showDeleteFolderConfirmation(folder, onUpdate)
                    2 -> handleSyncOption(folder, onUpdate)
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

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.edit_folder)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = editTextName.text.toString().trim()
                val newDescription = editTextDescription.text.toString().trim()

                if (newName.isNotEmpty()) {
                    val updatedFolder = folder.copy(
                        name = newName,
                        description = newDescription,
                        metadata = folder.metadata.copy(
                            syncStatus = SyncStatus.NotSynced
                        )
                    )
                    DataHandler.updateFolder(updatedFolder.id, newName, newDescription)
                    onUpdate()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteFolderConfirmation(folder: Folder, onUpdate: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_folder)
            .setMessage(R.string.delete_folder_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                DataHandler.deleteFolderWithSync(folder.id, context)
                onUpdate()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleSyncOption(folder: Folder, onUpdate: () -> Unit) {
        // Show sync progress dialog
        val progressDialog = DialogUtils.showProgressDialog(
            context = context,
            message = context.getString(R.string.syncing_folder),
            isCancelable = false
        )

        try {
            // Update folder sync status
            val updatedFolder = folder.copy(
                metadata = folder.metadata.copy(
                    syncStatus = SyncStatus.Synced
                )
            )
            DataHandler.updateFolder(updatedFolder.id, updatedFolder.name, updatedFolder.description)
            onUpdate()
        } catch (e: Exception) {
            // Handle sync error
            val errorFolder = folder.copy(
                metadata = folder.metadata.copy(
                    syncStatus = SyncStatus.SyncError(e.message ?: "Unknown error")
                )
            )
            DataHandler.updateFolder(errorFolder.id, errorFolder.name, errorFolder.description)
            onUpdate()
        } finally {
            progressDialog.dismiss()
        }
    }
}