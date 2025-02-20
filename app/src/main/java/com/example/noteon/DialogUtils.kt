package com.example.noteon

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object DialogUtils {

    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveButton: String = context.getString(R.string.confirm),
        negativeButton: String = context.getString(R.string.cancel),
        onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton) { _, _ -> onConfirm() }
            .setNegativeButton(negativeButton, null)
            .show()
    }

    fun showDeleteConfirmationDialog(
        context: Context,
        message: String,
        onConfirm: () -> Unit
    ) {
        showConfirmationDialog(
            context = context,
            title = context.getString(R.string.delete),
            message = message,
            positiveButton = context.getString(R.string.delete),
            onConfirm = onConfirm
        )
    }

    fun showProgressDialog(
        context: Context,
        message: String,
        isCancelable: Boolean = false
    ): AlertDialog {
        return MaterialAlertDialogBuilder(context)
            .setView(R.layout.dialog_progress)
            .setCancelable(isCancelable)
            .create()
            .apply {
                show()
                findViewById<TextView>(R.id.textViewProgress)?.text = message
                findViewById<TextView>(R.id.textViewPercentage)?.text = "0%"
            }
    }

    fun updateProgressDialog(
        dialog: AlertDialog,
        current: Int,
        total: Int,
        message: String? = null
    ) {
        val percentage = ((current.toFloat() / total) * 100).toInt()
        dialog.findViewById<TextView>(R.id.textViewPercentage)?.text = "$percentage%"
        message?.let {
            dialog.findViewById<TextView>(R.id.textViewProgress)?.text = it
        }
    }

    fun showInputDialog(
        context: Context,
        title: String,
        layoutResId: Int,
        positiveButton: String = context.getString(R.string.save),
        negativeButton: String = context.getString(R.string.cancel),
        onSetupViews: (View) -> Unit,
        onPositiveClick: (View) -> Boolean
    ) {
        val view = LayoutInflater.from(context).inflate(layoutResId, null)
        onSetupViews(view)

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(positiveButton) { dialog, _ ->
                if (!onPositiveClick(view)) {
                    dialog.dismiss()
                }
            }
            .setNegativeButton(negativeButton, null)
            .show()
    }

    fun showCreateFolderDialog(
        context: Context,
        existingFolder: Folder? = null,
        onSave: (name: String, description: String) -> Unit
    ) {
        showInputDialog(
            context = context,
            title = context.getString(
                if (existingFolder == null) R.string.create_folder
                else R.string.edit_folder
            ),
            layoutResId = R.layout.dialog_create_folder,
            positiveButton = context.getString(
                if (existingFolder == null) R.string.create
                else R.string.save
            ),
            onSetupViews = { view ->
                val editTextName = view.findViewById<EditText>(R.id.editTextFolderName)
                val editTextDescription = view.findViewById<EditText>(R.id.editTextFolderDescription)
                existingFolder?.let {
                    editTextName.setText(it.name)
                    editTextDescription.setText(it.description)
                }
            },
            onPositiveClick = { view ->
                val editTextName = view.findViewById<EditText>(R.id.editTextFolderName)
                val editTextDescription = view.findViewById<EditText>(R.id.editTextFolderDescription)
                val name = editTextName.text.toString().trim()
                val description = editTextDescription.text.toString().trim()

                if (name.isEmpty()) {
                    editTextName.error = context.getString(R.string.folder_name_required)
                    false
                } else {
                    onSave(name, description)
                    true
                }
            }
        )
    }

    fun showDeleteFolderConfirmationDialog(
        context: Context,
        onConfirm: () -> Unit
    ) {
        showDeleteConfirmationDialog(
            context = context,
            message = context.getString(R.string.delete_folder_confirmation),
            onConfirm = onConfirm
        )
    }

    fun showDiscardChangesDialog(
        context: Context,
        onDiscard: () -> Unit
    ) {
        showConfirmationDialog(
            context = context,
            title = context.getString(R.string.discard_changes),
            message = context.getString(R.string.discard_changes_message),
            positiveButton = context.getString(R.string.discard),
            onConfirm = onDiscard
        )
    }

    fun showEmptyTrashDialog(
        context: Context,
        onConfirm: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.empty_trash)
            .setMessage(R.string.empty_trash_message)
            .setPositiveButton(R.string.empty_trash) { _, _ -> onConfirm() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showExitGuestModeDialog(
        context: Context,
        onConfirm: () -> Unit
    ) {
        showConfirmationDialog(
            context = context,
            title = context.getString(R.string.exit_guest_mode),
            message = context.getString(R.string.exit_guest_mode_message),
            positiveButton = context.getString(R.string.exit),
            onConfirm = onConfirm
        )
    }

    fun showGuestDataFoundDialog(
        context: Context,
        onKeep: () -> Unit,
        onDiscard: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.guest_data_found)
            .setMessage(R.string.convert_guest_data_message)
            .setPositiveButton(R.string.convert) { _, _ -> onKeep() }
            .setNegativeButton(R.string.discard) { _, _ -> onDiscard() }
            .setCancelable(false)
            .show()
    }
}