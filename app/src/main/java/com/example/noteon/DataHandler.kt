package com.example.noteon

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

object DataHandler {
    private lateinit var dbHelper: DatabaseHelper

    // Initialize with context
    fun initialize(context: android.content.Context) {
        dbHelper = DatabaseHelper(context)
    }

    fun storeUserInfo(user: User) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(user.id ?: "")
            .set(user)
            .addOnSuccessListener {
                Log.d("DataHandler", "User profile created successfully")
            }
            .addOnFailureListener { e ->
                Log.w("DataHandler", "Error creating user profile", e)
            }
    }

    fun addNote(title: String, content: String, folderId: Long = 0): Note {
        val note = Note(
            id = 0, // SQLite will auto-generate the ID
            title = title,
            content = content,
            folderId = folderId
        )
        val id = dbHelper.addNote(note)
        return note.copy(id = id)
    }

    fun getAllNotes(): List<Note> = dbHelper.getAllNotes()

    fun getNoteById(id: Long): Note? = dbHelper.getNoteById(id)

    fun updateNote(updatedNote: Note) {
        dbHelper.updateNote(updatedNote)
    }

    fun toggleNoteFavorite(noteId: Long) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(isFavorite = !note.isFavorite)
            dbHelper.updateNote(updatedNote)
        }
    }

    fun getFavoriteNotes(): List<Note> = dbHelper.getFavoriteNotes()

    fun getNotesInFolder(folderId: Long): List<Note> = dbHelper.getNotesInFolder(folderId)

    fun moveNoteToTrash(noteId: Long) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(
                isDeleted = true,
                deletedDate = System.currentTimeMillis()
            )
            dbHelper.updateNote(updatedNote)
        }
    }

    fun restoreNoteFromTrash(noteId: Long) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(
                isDeleted = false,
                deletedDate = null
            )
            dbHelper.updateNote(updatedNote)
        }
    }

    fun getTrashNotes(): List<Note> = dbHelper.getTrashNotes()

    fun deleteNotePermanently(noteId: Long) {
        dbHelper.deleteNote(noteId)
    }

    fun emptyTrash() {
        dbHelper.emptyTrash()
    }

    // Folder operations
    fun createFolder(name: String, description: String): Folder {
        val folder = Folder(
            id = 0, // SQLite will auto-generate the ID
            name = name,
            description = description
        )
        val id = dbHelper.addFolder(folder)
        return folder.copy(id = id)
    }

    fun getAllFolders(): List<Folder> = dbHelper.getAllFolders()

    fun getFolderById(id: Long): Folder? = dbHelper.getFolderById(id)

    fun updateFolder(folderId: Long, newName: String, newDescription: String) {
        getFolderById(folderId)?.let { folder ->
            val updatedFolder = folder.copy(
                name = newName,
                description = newDescription
            )
            dbHelper.updateFolder(updatedFolder)
        }
    }

    fun deleteFolder(folderId: Long) {
        dbHelper.deleteFolder(folderId)
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(folderId = folderId)
            dbHelper.updateNote(updatedNote)
        }
    }

    fun searchFolders(query: String): List<Folder> = dbHelper.searchFolders(query)

    fun markNoteAsSynced(noteId: Long, userId: String) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(isSynced = true, userId = userId)
            dbHelper.updateNote(updatedNote)
        }
    }

    fun addNoteFromSync(note: Note): Note {
        val id = dbHelper.addNote(note)
        return note.copy(id = id)
    }
}
