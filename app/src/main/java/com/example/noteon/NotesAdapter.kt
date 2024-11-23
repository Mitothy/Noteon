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
    private val onAIOptions: (Note) -> Unit
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTitle: TextView = itemView.findViewById(R.id.textViewTitle)
        private val textViewContent: TextView = itemView.findViewById(R.id.textViewContent)
        private val buttonOptions: MaterialButton = itemView.findViewById(R.id.buttonOptions)
        private val buttonAIOptions: MaterialButton = itemView.findViewById(R.id.buttonAIOptions)

        fun bind(note: Note) {
            textViewTitle.text = buildSpannedString {
                append(note.title)
                if (note.isFavorite) {
                    append(" ★")
                }
            }
            textViewContent.text = note.content

            itemView.setOnClickListener { onNoteClick(note) }
            buttonOptions.setOnClickListener {
                NoteOptionsDialog(itemView.context, coroutineScope).show(note) {
                    notifyDataSetChanged()
                }
            }
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