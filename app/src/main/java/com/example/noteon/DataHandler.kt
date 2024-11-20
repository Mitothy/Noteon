package com.example.noteon

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object DataHandler {
    private lateinit var dbHelper: DatabaseHelper
    private var usersMap = mutableMapOf<String, User>()

    // Initialize with context
    fun initialize(context: Context) {
        dbHelper = DatabaseHelper(context)
    }

    fun storeUserInfo(user: User) {
        // Store in local map
        usersMap[user.id ?: ""] = user

        // Store in Firebase
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

    fun addNote(context: Context, title: String, content: String, folderId: Long = 0): Note {
        val guestSession = GuestSession.getInstance(context)
        val note = Note(
            id = 0, // SQLite will auto-generate the ID
            title = title,
            content = content,
            folderId = folderId,
            userId = if (guestSession.isGuestSession()) {
                guestSession.getGuestId()
            } else {
                AuthManager.getInstance(context).currentUser?.uid
            }
        )
        val id = dbHelper.addNote(note)
        return note.copy(id = id)
    }

    fun clearGuestData(guestId: String) {
        // Get all notes for the guest user
        val guestNotes = getAllNotes().filter { it.userId == guestId }

        // Delete each guest note
        guestNotes.forEach { note ->
            deleteNotePermanently(note.id)
        }

        // Also delete any folders created by guest
        getAllFolders().forEach { folder ->
            // Since folders don't have userId, we'll delete empty folders
            if (getNotesInFolder(folder.id).isEmpty()) {
                deleteFolder(folder.id)
            }
        }
    }

    suspend fun backupNotes(context: Context) {
        val authManager = AuthManager.getInstance(context)

        authManager.currentUser?.let { user ->
            val notes = getAllNotes()
            notes.forEach { note ->
                // Only backup non-deleted notes that belong to the authenticated user
                if (!note.isDeleted && note.userId == user.uid) {
                    // Create a map of note data
                    val noteMap = hashMapOf(
                        "id" to note.id,
                        "title" to note.title,
                        "content" to note.content,
                        "folderId" to note.folderId,
                        "isFavorite" to note.isFavorite,
                        "timestamp" to note.timestamp,
                        "userId" to user.uid
                    )

                    // Save to Firebase
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users")
                        .document(user.uid)
                        .collection("notes")
                        .document(note.id.toString())
                        .set(noteMap)
                        .await()

                    // Mark note as synced
                    markNoteAsSynced(note.id, user.uid)
                }
            }
        }
    }

    fun convertGuestNotesToUser(guestId: String, userId: String) {
        val guestNotes = getAllNotes().filter { it.userId == guestId }
        guestNotes.forEach { note ->
            val updatedNote = note.copy(userId = userId)
            dbHelper.updateNote(updatedNote)
        }
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

    fun getUserName(userId: String): String? {
        return usersMap[userId]?.name
    }
}