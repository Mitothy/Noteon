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
        clearSyncedNotes()
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
                // First, clear any existing synced notes for this user
                DataHandler.getAllNotes()
                    .filter { it.userId == user.uid }
                    .forEach { DataHandler.deleteNotePermanently(it.id) }

                val notesSnapshot = database
                    .child("users")
                    .child(user.uid)
                    .child("notes")
                    .get()
                    .await()

                for (noteSnapshot in notesSnapshot.children) {
                    try {
                        val noteMap = noteSnapshot.value as? Map<*, *>
                        if (noteMap != null) {
                            val note = Note(
                                id = 0L, // Let SQLite generate a new ID
                                title = (noteMap["title"] as? String) ?: "",
                                content = (noteMap["content"] as? String) ?: "",
                                folderId = (noteMap["folderId"] as? Long) ?: 0L,
                                isFavorite = (noteMap["isFavorite"] as? Boolean) ?: false,
                                timestamp = (noteMap["timestamp"] as? Long) ?: System.currentTimeMillis(),
                                userId = user.uid,
                                isSynced = true
                            )

                            // Use addNoteFromSync to add the note to local database
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
}