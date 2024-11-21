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

                val serverNoteIds = notesSnapshot.children
                    .mapNotNull { (it.value as? Map<*, *>)?.get("id") as? Long }
                    .toSet()

                val localNotes = DataHandler.getAllNotes()
                    .filter { it.userId == user.uid && it.isSynced }

                localNotes.forEach { note ->
                    if (note.id !in serverNoteIds) {
                        DataHandler.deleteNotePermanently(note.id)
                    }
                }

                notesSnapshot.children.forEach { noteSnapshot ->
                    try {
                        val noteMap = noteSnapshot.value as? Map<*, *>
                        if (noteMap != null) {
                            val noteId = (noteMap["id"] as? Long)
                            if (noteId == null) {
                                return@forEach
                            }

                            val note = Note(
                                id = noteId,
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
                        return@forEach
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
                // Get reference to all folders for this user
                val userFoldersRef = database
                    .child("users")
                    .child(user.uid)
                    .child("folders")

                // Get current folders in Firebase
                val foldersSnapshot = userFoldersRef.get().await()

                // Get all folder IDs that exist in Firebase
                val firebaseFolderIds = foldersSnapshot.children.mapNotNull {
                    it.child("id").getValue(Long::class.java)
                }.toSet()

                // Get all local folder IDs
                val localFolderIds = DataHandler.getFoldersByUser(context)
                    .map { it.id }
                    .toSet()

                // Delete folders from Firebase that don't exist locally
                firebaseFolderIds.forEach { folderId ->
                    if (folderId !in localFolderIds) {
                        userFoldersRef.child(folderId.toString())
                            .removeValue()
                            .await()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing folder deletions", e)
                throw e
            }
        }
    }


    suspend fun restoreFolders() {
        currentUser?.let { user ->
            try {
                // Clear all synced folders first
                DataHandler.clearSyncedFolders(user.uid)

                val foldersSnapshot = database
                    .child("users")
                    .child(user.uid)
                    .child("folders")
                    .get()
                    .await()

                // Only restore folders that exist on server
                foldersSnapshot.children.forEach { folderSnapshot ->
                    try {
                        val folderMap = folderSnapshot.value as? Map<*, *>
                        if (folderMap != null) {
                            val folder = Folder(
                                id = (folderMap["id"] as? Long) ?: return@forEach,
                                name = (folderMap["name"] as? String) ?: "",
                                description = (folderMap["description"] as? String) ?: "",
                                timestamp = (folderMap["timestamp"] as? Long) ?: System.currentTimeMillis(),
                                userId = user.uid,
                                isSynced = true
                            )
                            if (folder.name.isNotEmpty()) {
                                DataHandler.addFolderFromSync(folder)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring folder: ${folderSnapshot.key}", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring folders", e)
                throw e
            }
        }
    }
}