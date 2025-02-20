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
        val authState = AuthStateManager.getInstance(context).getCurrentState()
        val userId = when (authState) {
            is AuthState.Authenticated -> authState.user.uid
            is AuthState.Guest -> GuestSession.getInstance(context).getGuestId()
            else -> null
        }

        val metadata = NoteMetadata(
            folderId = folderId,
            userId = userId,
            syncStatus = SyncStatus.NotSynced
        )

        val note = Note(
            id = 0,
            title = title,
            content = content,
            state = NoteState.Active,
            metadata = metadata
        )
        val id = dbHelper.addNote(note)
        return note.copy(id = id)
    }

    fun clearGuestData(guestId: String) {
        getAllNotes().filter { it.metadata.userId == guestId }.forEach { note ->
            deleteNotePermanently(note.id)
        }

        getAllFolders().filter { it.metadata.userId == guestId }.forEach { folder ->
            deleteFolder(folder.id)
        }
    }

    fun clearUserData(userId: String) {
        getAllNotes().filter { it.metadata.userId == userId }.forEach { note ->
            deleteNotePermanently(note.id)
        }

        getAllFolders().filter { it.metadata.userId == userId }.forEach { folder ->
            deleteFolder(folder.id)
        }
    }

    fun convertGuestNotesToUser(guestId: String, userId: String) {
        val guestNotes = getAllNotes().filter { it.metadata.userId == guestId }
        guestNotes.forEach { note ->
            val updatedNote = note.copy(
                metadata = note.metadata.copy(userId = userId)
            )
            dbHelper.updateNote(updatedNote)
        }
    }

    fun getAllNotes(): List<Note> = dbHelper.getAllNotes().filter { !it.isTrashed() }

    fun getNoteById(id: Long): Note? = dbHelper.getNoteById(id)

    fun updateNote(updatedNote: Note) {
        dbHelper.updateNote(updatedNote)
    }

    fun getActiveNotes(): List<Note> = dbHelper.getActiveNotes()

    fun getFavoriteNotes(): List<Note> = dbHelper.getFavoriteNotes()

    fun getNotesInFolder(folderId: Long): List<Note> = dbHelper.getNotesInFolder(folderId)

    fun moveNoteToTrash(noteId: Long) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.moveToTrash()
            dbHelper.updateNote(updatedNote)
        }
    }

    fun restoreNoteFromTrash(noteId: Long) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.restore()
            dbHelper.updateNote(updatedNote)
        }
    }

    fun getTrashNotes(): List<Note> = dbHelper.getTrashNotes()

    fun deleteNotePermanently(noteId: Long) {
        getNoteById(noteId)?.let { note ->
            if (note.metadata.folderId != 0L) {
                moveNoteToFolder(noteId, 0)
            }
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

        val metadata = FolderMetadata(
            userId = userId,
            syncStatus = SyncStatus.NotSynced
        )

        val folder = Folder(
            id = 0,
            name = name,
            description = description,
            metadata = metadata
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

        // Only return folders that explicitly match the current user's ID
        return dbHelper.getFoldersByUser(currentUserId ?: "")
            .filter { it.userId == currentUserId }
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
        val notesInFolder = getNotesInFolder(folderId)
        notesInFolder.forEach { note ->
            moveNoteToFolder(note.id, 0)
        }
        dbHelper.deleteFolder(folderId)
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long) {
        getNoteById(noteId)?.let { note ->
            // Verify folder exists if we're moving to a folder
            if (folderId != 0L && getFolderById(folderId) == null) {
                return
            }

            val updatedNote = note.copy(
                metadata = note.metadata.copy(
                    folderId = folderId,
                    syncStatus = SyncStatus.NotSynced
                )
            )
            dbHelper.updateNote(updatedNote)
        }
    }

    fun searchFolders(query: String): List<Folder> = dbHelper.searchFolders(query)

    fun addFolderFromSync(folder: Folder): Folder {
        // First check if folder exists
        val existingFolder = getFolderById(folder.id)

        // If folder exists, update it instead of recreating
        if (existingFolder != null) {
            val updatedFolder = folder.withSyncStatus(SyncStatus.Synced)
            dbHelper.updateFolder(updatedFolder)
            return updatedFolder
        }

        // If folder doesn't exist, create it
        val syncedFolder = folder.withSyncStatus(SyncStatus.Synced)
        val id = dbHelper.upsertSyncedFolder(syncedFolder)
        return syncedFolder.copy(id = id)
    }

    fun markNoteAsSynced(noteId: Long, userId: String) {
        getNoteById(noteId)?.let { note ->
            val updatedNote = note.copy(
                metadata = note.metadata.copy(
                    userId = userId,
                    syncStatus = SyncStatus.Synced
                )
            )
            dbHelper.updateNote(updatedNote)
        }
    }

    fun addNoteFromSync(note: Note): Note {
        val syncedNote = note.copy(
            metadata = note.metadata.copy(syncStatus = SyncStatus.Synced)
        )
        val id = dbHelper.upsertSyncedNote(syncedNote)
        return syncedNote.copy(id = id)
    }

    fun getUserName(userId: String): String? {
        return usersMap[userId]?.name
    }

    fun deleteNoteWithSync(noteId: Long, context: Context) {
        val authManager = AuthManager.getInstance(context)
        val note = getNoteById(noteId)

        if (note != null && authManager.currentUser?.uid == note.metadata.userId) {
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
                if (note.metadata.userId == authManager.currentUser?.uid) {
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

        if (folder != null && authManager.currentUser?.uid == folder.metadata.userId) {
            deleteFolder(folderId)

            database.child("users")
                .child(authManager.currentUser!!.uid)
                .child("folders")
                .child(folderId.toString())
                .removeValue()
                .addOnFailureListener { e ->
                    Log.e("DataHandler", "Failed to delete folder from Firebase: ${e.message}")
                }
        } else {
            deleteFolder(folderId)
        }
    }
}