package com.example.noteon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class FolderAdapter(
    private var folders: List<Folder>,
    private val onFolderClick: (Folder) -> Unit,
    private val onFolderOptions: (Folder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFolderName: TextView = itemView.findViewById(R.id.textViewFolderName)
        private val textViewFolderDescription: TextView = itemView.findViewById(R.id.textViewFolderDescription)
        private val textViewNoteCount: TextView = itemView.findViewById(R.id.textViewNoteCount)
        private val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonFolderEdit)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonFolderDelete)

        fun bind(folder: Folder) {
            textViewFolderName.text = folder.name
            textViewFolderDescription.text = folder.description
            val noteCount = DataHandler.getNotesInFolder(folder.id).size
            textViewNoteCount.text = "$noteCount notes"

            itemView.setOnClickListener { onFolderClick(folder) }

            buttonEdit.setOnClickListener { onFolderOptions(folder) }

            buttonDelete.setOnClickListener {
                AlertDialog.Builder(itemView.context)
                    .setTitle(R.string.delete_folder)
                    .setMessage(R.string.delete_folder_confirmation)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        DataHandler.deleteFolder(folder.id)
                        updateFolders(folders.filter { it.id != folder.id })
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount(): Int = folders.size

    fun updateFolders(newFolders: List<Folder>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}