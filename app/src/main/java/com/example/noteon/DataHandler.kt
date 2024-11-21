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


    fun markFolderAsSynced(folderId: Long, userId: String) {
        getFolderById(folderId)?.let { folder ->
            val updatedFolder = folder.copy(
                isSynced = true,
                userId = userId
            )
            dbHelper.updateFolder(updatedFolder)
        }
    }

    fun markNoteAsSynced(noteId: Long, userId: String) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(isSynced = true, userId = userId)
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


}