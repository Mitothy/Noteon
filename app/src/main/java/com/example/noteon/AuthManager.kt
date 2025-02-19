package com.example.noteon

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthManager private constructor(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

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

    fun signOut() {
        // Clear settings before signing out
        PreferencesManager.getInstance(context).clearSettings()

        // Clear data for current user before signing out
        currentUser?.let { user ->
            DataHandler.clearUserData(user.uid)
        }
        auth.signOut()
    }

    suspend fun restoreData() {
        currentUser?.let { user ->
            try {
                // First restore settings
                PreferencesManager.getInstance(context).restoreSettingsFromFirebase(user.uid)

                // Then restore folders to ensure proper hierarchy
                restoreFolders()

                // Finally restore notes
                restoreNotes()

                Log.d(TAG, "All data restored successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during data restoration", e)
                throw e
            }
        }
    }

    suspend fun backupData(onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }) {
        currentUser?.let { user ->
            try {
                // First backup settings
                PreferencesManager.getInstance(context).syncSettingsToFirebase(user.uid)

                // Then backup folders
                backupFolders()

                // Finally backup notes with progress
                backupNotes(onProgress)

                Log.d(TAG, "All data backed up successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error during data backup", e)
                throw e
            }
        }
    }

    suspend fun backupNotes(onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }) {
        currentUser?.let { user ->
            try {
                val notes = DataHandler.getAllNotes()
                val totalNotes = notes.count { !it.isTrashed() && it.metadata.userId == user.uid }
                var currentNote = 0

                notes.forEach { note ->
                    if (!note.isTrashed() && note.metadata.userId == user.uid) {
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
                                "folderId" to note.metadata.folderId,
                                "state" to NoteState.toDatabaseValue(note.state),
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
                    .filter { it.metadata.userId == user.uid && it.isSynced }

                localNotes.forEach { note ->
                    if (note.id !in serverNoteIds) {
                        DataHandler.deleteNotePermanently(note.id)
                    }
                }

                notesSnapshot.children.forEach { noteSnapshot ->
                    try {
                        val noteMap = noteSnapshot.value as? Map<*, *>
                        if (noteMap != null) {
                            val noteId = (noteMap["id"] as? Long) ?: return@forEach

                            val state = NoteState.fromDatabaseValue(
                                (noteMap["state"] as? Int) ?: 0,
                                (noteMap["deletedDate"] as? Long)
                            )

                            val metadata = NoteMetadata(
                                folderId = (noteMap["folderId"] as? Long) ?: 0L,
                                userId = user.uid,
                                syncStatus = SyncStatus.Synced
                            )

                            val note = Note(
                                id = noteId,
                                title = (noteMap["title"] as? String) ?: "",
                                content = (noteMap["content"] as? String) ?: "",
                                state = state,
                                metadata = metadata,
                                timestamp = (noteMap["timestamp"] as? Long)
                                    ?: System.currentTimeMillis()
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
                val userFoldersRef = database
                    .child("users")
                    .child(user.uid)
                    .child("folders")

                // Get all folders for current user
                val folders = DataHandler.getFoldersByUser(context)

                // Upload each folder
                folders.forEach { folder ->
                    val folderMap = hashMapOf(
                        "id" to folder.id,
                        "name" to folder.name,
                        "description" to folder.description,
                        "timestamp" to folder.timestamp,
                        "userId" to user.uid,
                        "metadata" to mapOf(
                            "userId" to folder.metadata.userId,
                            "syncStatus" to when (folder.metadata.syncStatus) {
                                is SyncStatus.Synced -> 1
                                is SyncStatus.SyncError -> 2
                                else -> 0
                            }
                        )
                    )

                    userFoldersRef.child(folder.id.toString())
                        .setValue(folderMap)
                        .await()
                }

                // Get all folder IDs in Firebase
                val foldersSnapshot = userFoldersRef.get().await()
                val firebaseFolderIds = foldersSnapshot.children
                    .mapNotNull { it.child("id").getValue(Long::class.java) }
                    .toSet()

                // Delete folders from Firebase that don't exist locally
                val localFolderIds = folders.map { it.id }.toSet()
                firebaseFolderIds.forEach { folderId ->
                    if (folderId !in localFolderIds) {
                        userFoldersRef.child(folderId.toString())
                            .removeValue()
                            .await()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error backing up folders", e)
                throw e
            }
        }
    }

    suspend fun restoreFolders() {
        currentUser?.let { user ->
            try {
                // Clear existing synced folders
                DataHandler.clearSyncedFolders(user.uid)

                val foldersSnapshot = database
                    .child("users")
                    .child(user.uid)
                    .child("folders")
                    .get()
                    .await()

                foldersSnapshot.children.forEach { folderSnapshot ->
                    try {
                        val folderMap = folderSnapshot.value as? Map<*, *>
                        if (folderMap != null) {
                            val metadataMap = folderMap["metadata"] as? Map<*, *>

                            val metadata = FolderMetadata(
                                userId = user.uid,
                                syncStatus = when (metadataMap?.get("syncStatus") as? Int) {
                                    1 -> SyncStatus.Synced
                                    2 -> SyncStatus.SyncError("Unknown error")
                                    else -> SyncStatus.NotSynced
                                }
                            )

                            val folder = Folder(
                                id = (folderMap["id"] as? Long) ?: return@forEach,
                                name = (folderMap["name"] as? String) ?: "",
                                description = (folderMap["description"] as? String) ?: "",
                                timestamp = (folderMap["timestamp"] as? Long)
                                    ?: System.currentTimeMillis(),
                                metadata = metadata
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