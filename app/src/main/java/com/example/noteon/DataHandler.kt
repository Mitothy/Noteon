package com.example.noteon

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

object DataHandler {
    private lateinit var dbHelper: DatabaseHelper
    private var usersMap = mutableMapOf<String, User>()
    private val database = FirebaseDatabase.getInstance().reference

    // Initialize with context
    fun initialize(context: Context) {
        dbHelper = DatabaseHelper(context)
    }

    fun storeUserInfo(user: User) {
        usersMap[user.id] = user
        database.child("users")
            .child(user.id)
            .setValue(user)
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
        // ONLY clear notes and folders associated with this specific guest ID
        getAllNotes().filter { it.userId == guestId }.forEach { note ->
            deleteNotePermanently(note.id)
        }

        getAllFolders().filter { it.userId == guestId }.forEach { folder ->
            deleteFolder(folder.id)
        }
    }

    fun clearUserData(userId: String) {
        // Clear all notes for this user
        getAllNotes().filter { it.userId == userId }.forEach { note ->
            deleteNotePermanently(note.id)
        }

        // Clear all folders for this user
        getAllFolders().filter { it.userId == userId }.forEach { folder ->
            deleteFolder(folder.id)
        }
    }

    fun convertGuestNotesToUser(guestId: String, userId: String) {
        val guestNotes = getAllNotes().filter { it.userId == guestId }
        guestNotes.forEach { note ->
            val updatedNote = note.copy(userId = userId)
            dbHelper.updateNote(updatedNote)
        }
    }

    fun getAllNotes(): List<Note> {
        return dbHelper.getAllNotes().filter { !it.isDeleted }
    }

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

    fun getNotesInFolder(folderId: Long): List<Note> {
        return dbHelper.getNotesInFolder(folderId).filter { !it.isDeleted }
    }

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
        // First check if note exists
        getNoteById(noteId)?.let { note ->
            // Remove from folder if it's in one
            if (note.folderId != 0L) {
                moveNoteToFolder(noteId, 0)
            }
            // Delete the note
            dbHelper.deleteNote(noteId)
        }
    }

    fun emptyTrash() {
        dbHelper.emptyTrash()
    }

    fun createFolder(name: String, description: String, context: Context): Folder {
        val authManager = AuthManager.getInstance(context)
        val guestSession = GuestSession.getInstance(context)

        val userId = if (guestSession.isGuestSession()) {
            guestSession.getGuestId()
        } else {
            authManager.currentUser?.uid
        }

        val folder = Folder(
            id = 0,
            name = name,
            description = description,
            userId = userId   // Store the userId with the folder
        )
        val id = dbHelper.addFolder(folder)
        return folder.copy(id = id)
    }

    fun getFoldersByUser(context: Context): List<Folder> {
        val authManager = AuthManager.getInstance(context)
        val guestSession = GuestSession.getInstance(context)

        val currentUserId = if (guestSession.isGuestSession()) {
            guestSession.getGuestId()
        } else {
            authManager.currentUser?.uid
        }

        // Only return folders for the current user
        return dbHelper.getAllFolders().filter { it.userId == currentUserId }
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
        // First get all notes in this folder
        val notesInFolder = getNotesInFolder(folderId)

        // Move all notes to root (folderId = 0)
        notesInFolder.forEach { note ->
            moveNoteToFolder(note.id, 0)
        }

        // Delete the folder
        dbHelper.deleteFolder(folderId)
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(folderId = folderId)
            dbHelper.updateNote(updatedNote)
        }
    }

    fun searchFolders(query: String): List<Folder> = dbHelper.searchFolders(query)

    fun addFolderFromSync(folder: Folder): Folder {
        val id = dbHelper.upsertSyncedFolder(folder)
        return folder.copy(id = id)
    }

    fun markNoteAsSynced(noteId: Long, userId: String) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(
                isSynced = true,
                userId = userId
            )
            dbHelper.updateNote(updatedNote)
        }
    }

    fun addNoteFromSync(note: Note): Note {
        val id = dbHelper.upsertSyncedNote(note)
        return note.copy(id = id)
    }


    fun getUserName(userId: String): String? {
        return usersMap[userId]?.name
    }

    fun deleteNoteWithSync(noteId: Long, context: Context) {
        val authManager = AuthManager.getInstance(context)
        val note = getNoteById(noteId)

        if (note != null && authManager.currentUser?.uid == note.userId) {
            database.child("users")
                .child(authManager.currentUser!!.uid)
                .child("notes")
                .child(noteId.toString())
                .removeValue()
                .addOnSuccessListener {
                    Log.d("DataHandler", "Note deleted from Firebase")
                }
                .addOnFailureListener { e ->
                    Log.w("DataHandler", "Error deleting note from Firebase", e)
                }
        }

        deleteNotePermanently(noteId)
    }

    fun emptyTrashWithSync(context: Context) {
        val authManager = AuthManager.getInstance(context)
        val trashNotes = getTrashNotes()

        if (authManager.currentUser != null) {
            trashNotes.forEach { note ->
                if (note.userId == authManager.currentUser?.uid) {
                    database.child("users")
                        .child(authManager.currentUser!!.uid)
                        .child("notes")
                        .child(note.id.toString())
                        .removeValue()
                        .addOnSuccessListener {
                            Log.d("DataHandler", "Note ${note.id} deleted from Firebase")
                        }
                        .addOnFailureListener { e ->
                            Log.w("DataHandler", "Error deleting note ${note.id} from Firebase", e)
                        }
                }
            }
        }

        emptyTrash()
    }

    fun deleteFolderWithSync(folderId: Long, context: Context) {
        val authManager = AuthManager.getInstance(context)
        val folder = getFolderById(folderId)

        if (folder != null && authManager.currentUser?.uid == folder.userId) {
            // First delete locally
            deleteFolder(folderId)

            // Then remove from Firebase
            database.child("users")
                .child(authManager.currentUser!!.uid)
                .child("folders")
                .child(folderId.toString())
                .removeValue()
                .addOnFailureListener { e ->
                    Log.e("DataHandler", "Failed to delete folder from Firebase: ${e.message}")
                }
        } else {
            // If not synced or guest user, just delete locally
            deleteFolder(folderId)
        }
    }

    fun clearSyncedFolders(userId: String) {
        getAllFolders()
            .filter { it.userId == userId && it.isSynced }
            .forEach { folder ->
                deleteFolder(folder.id)
            }
    }

}