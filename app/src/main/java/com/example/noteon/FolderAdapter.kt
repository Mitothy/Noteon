package com.example.noteon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FolderAdapter(
    private var folders: List<Folder>,
    private val onFolderClick: (Folder) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFolderName: TextView = itemView.findViewById(R.id.textViewFolderName)
        private val textViewNoteCount: TextView = itemView.findViewById(R.id.textViewNoteCount)

        fun bind(folder: Folder) {
            textViewFolderName.text = folder.name
            val noteCount = DataHandler.getNotesInFolder(folder.id).size
            textViewNoteCount.text = "$noteCount notes"
            itemView.setOnClickListener { onFolderClick(folder) }
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