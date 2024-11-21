package com.example.noteon

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.GoogleAuthProvider

class AuthManager private constructor(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val dbHelper: DatabaseHelper = DatabaseHelper(context)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    companion object {
        private const val TAG = "AuthManager"

        @Volatile
        private var instance: AuthManager? = null

        fun getInstance(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    suspend fun signInWithCredentials(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()

            // After successful sign in, restore notes from Firebase
            if (result.user != null) {
                restoreNotes()
            }

            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        // Clear data for current user before signing out
        currentUser?.let { user ->
            DataHandler.clearUserData(user.uid)
        }
        auth.signOut()
    }

    fun clearSyncedNotes() {
        currentUser?.let { user ->
            val notes = DataHandler.getAllNotes()
            notes.filter { it.userId == user.uid }.forEach { note ->
                DataHandler.deleteNotePermanently(note.id)
            }
        }
    }

    suspend fun backupNotes(onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }) {
        currentUser?.let { user ->
            try {
                val notes = DataHandler.getAllNotes()
                val totalNotes = notes.count { !it.isDeleted && it.userId == user.uid }
                var currentNote = 0

                notes.forEach { note ->
                    if (!note.isDeleted && note.userId == user.uid) {
                        try {
                            val noteRef = database
                                .child("users")
                                .child(user.uid)
                                .child("notes")
                                .child(note.id.toString())

                            val noteMap = hashMapOf(
                                "id" to note.id,
                                "title" to note.title,
                                "content" to note.content,
                                "folderId" to note.folderId,
                                "isFavorite" to note.isFavorite,
                                "timestamp" to note.timestamp,
                                "userId" to user.uid
                            )

                            noteRef.setValue(noteMap).await()
                            DataHandler.markNoteAsSynced(note.id, user.uid)

                            currentNote++
                            onProgress(currentNote, totalNotes)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error backing up note ${note.id}", e)
                            throw e
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during backup", e)
                throw e
            }
        }
    }

    suspend fun restoreNotes() {
        currentUser?.let { user ->
            try {
                val notesSnapshot = database
                    .child("users")
                    .child(user.uid)
                    .child("notes")
                    .get()
                    .await()

                // Get all server note IDs first
                val serverNoteIds = notesSnapshot.children
                    .mapNotNull { (it.value as? Map<*, *>)?.get("id") as? Long }
                    .toSet()

                // Delete local notes that don't exist on server
                val localNotes = DataHandler.getAllNotes()
                    .filter { it.userId == user.uid && it.isSynced }

                localNotes.forEach { note ->
                    if (note.id !in serverNoteIds) {
                        DataHandler.deleteNotePermanently(note.id)
                    }
                }

                // Now process server notes
                for (noteSnapshot in notesSnapshot.children) {
                    try {
                        val noteMap = noteSnapshot.value as? Map<*, *>
                        if (noteMap != null) {
                            val note = Note(
                                id = (noteMap["id"] as? Long) ?: continue,
                                title = (noteMap["title"] as? String) ?: "",
                                content = (noteMap["content"] as? String) ?: "",
                                folderId = (noteMap["folderId"] as? Long) ?: 0L,
                                isFavorite = (noteMap["isFavorite"] as? Boolean) ?: false,
                                timestamp = (noteMap["timestamp"] as? Long)
                                    ?: System.currentTimeMillis(),
                                userId = user.uid,
                                isSynced = true
                            )

                            if (note.title.isNotEmpty()) {
                                DataHandler.addNoteFromSync(note)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring note: ${noteSnapshot.key}", e)
                        continue
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring notes", e)
                throw e
            }
        }
    }

    suspend fun backupFolders() {
        currentUser?.let { user ->
            try {
                val folders = DataHandler.getFoldersByUser(context)

                folders.forEach { folder ->
                    // Only backup non-synced folders that belong to the authenticated user
                    if (!folder.isSynced && folder.userId == user.uid) {
                        val folderRef = database
                            .child("users")
                            .child(user.uid)
                            .child("folders")
                            .child(folder.id.toString())

                        val folderMap = hashMapOf(
                            "id" to folder.id,
                            "name" to folder.name,
                            "description" to folder.description,
                            "timestamp" to folder.timestamp,
                            "userId" to user.uid
                        )

                        folderRef.setValue(folderMap).await()
                        DataHandler.markFolderAsSynced(folder.id, user.uid)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during folder backup", e)
                throw e
            }
        }
    }

    suspend fun restoreFolders() {
        Log.d(TAG, "Starting folder restoration")
        currentUser?.let { user ->
            try {
                // Get reference to user's folders in Firebase
                val userFoldersRef = database
                    .child("users")
                    .child(user.uid)
                    .child("folders")

                // Get the snapshot of all folders
                val foldersSnapshot = userFoldersRef.get().await()

                // Create a map of server folder IDs and their data
                Log.d(TAG, "Retrieved folders snapshot: ${foldersSnapshot.value}")
                val serverFolders = mutableMapOf<Long, Map<String, Any>>()
                foldersSnapshot.children.forEach { folderSnapshot ->
                    val folderMap = folderSnapshot.value as? Map<*, *>
                    val folderId = (folderMap?.get("id") as? Number)?.toLong()
                    if (folderId != null) {
                        @Suppress("UNCHECKED_CAST")
                        serverFolders[folderId] = folderMap as Map<String, Any>
                    }
                }

                // Get local folders for this user
                val localFolders = DataHandler.getFoldersByUser(context)
                    .filter { it.userId == user.uid }
                    .associateBy { it.id }

                // Process each server folder
                serverFolders.forEach { (folderId, folderData) ->
                    try {
                        val folder = Folder(
                            id = folderId,
                            name = folderData["name"] as? String ?: "",
                            description = folderData["description"] as? String ?: "",
                            timestamp = (folderData["timestamp"] as? Number)?.toLong()
                                ?: System.currentTimeMillis(),
                            userId = user.uid,
                            isSynced = true
                        )

                        if (folder.name.isNotEmpty()) {
                            DataHandler.addFolderFromSync(folder)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring folder $folderId", e)
                    }
                }

                // Remove local folders that don't exist on server
                localFolders.forEach { (folderId, folder) ->
                    if (!serverFolders.containsKey(folderId) && folder.isSynced) {
                        DataHandler.deleteFolder(folderId)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error restoring folders", e)
                throw e
            }
        }
    }
}