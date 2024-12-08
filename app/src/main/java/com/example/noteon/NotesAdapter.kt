package com.example.noteon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope

class NotesAdapter(
    private var notes: List<Note>,
    private val coroutineScope: CoroutineScope,
    private val onNoteClick: (Note) -> Unit,
    private val onAIOptions: (Note) -> Unit,
    private val isTrashView: Boolean = false
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        private val textViewContent: TextView = itemView.findViewById(R.id.textViewContent)
        private val buttonOptions: MaterialButton = itemView.findViewById(R.id.buttonOptions)
        private val buttonAIOptions: MaterialButton = itemView.findViewById(R.id.buttonAIOptions)

        fun bind(note: Note) {
            textViewTitle.text = buildSpannedString {
                append(note.title)
                if (note.isFavorite && !isTrashView) {
                    append(" ★")
                }
            }
            MarkdownUtils.renderPreview(textViewContent, note.content)

            itemView.setOnClickListener { onNoteClick(note) }
            buttonOptions.setOnClickListener {
                NoteOptionsDialog(itemView.context, coroutineScope).show(note, isTrashView) {
                    val updatedNotes = if (isTrashView) {
                        DataHandler.getTrashNotes()
                    } else {
                        when {
                            note.folderId != 0L -> DataHandler.getNotesInFolder(note.folderId)
                            note.isFavorite -> DataHandler.getFavoriteNotes()
                            else -> DataHandler.getAllNotes()
                        }
                    }
                    updateNotes(updatedNotes)
                }
            }
            // Hide AI options in trash view
            buttonAIOptions.visibility = if (isTrashView) View.GONE else View.VISIBLE
            buttonAIOptions.setOnClickListener { onAIOptions(note) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }
}