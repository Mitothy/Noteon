package com.example.noteon

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.GoogleAuthProvider

class AuthManager private constructor(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
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
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
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

    suspend fun backupNotes() {
        currentUser?.let { user ->
            val notes = DataHandler.getAllNotes()
            notes.forEach { note ->
                // Only backup non-deleted notes
                if (!note.isDeleted) {
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
                    database.reference
                        .child("users")
                        .child(user.uid)
                        .child("notes")
                        .child(note.id.toString())
                        .setValue(noteMap)
                        .await()

                    // Mark note as synced
                    DataHandler.markNoteAsSynced(note.id, user.uid)
                }
            }
        }
    }

    suspend fun restoreNotes() {
        currentUser?.let { user ->
            val snapshot = database.reference
                .child("users")
                .child(user.uid)
                .child("notes")
                .get()
                .await()

            snapshot.children.forEach { noteSnapshot ->
                val noteMap = noteSnapshot.value as? Map<*, *>
                if (noteMap != null) {
                    val note = Note(
                        id = (noteMap["id"] as? Long) ?: 0L,
                        title = (noteMap["title"] as? String) ?: "",
                        content = (noteMap["content"] as? String) ?: "",
                        folderId = (noteMap["folderId"] as? Long) ?: 0L,
                        isFavorite = (noteMap["isFavorite"] as? Boolean) ?: false,
                        timestamp = (noteMap["timestamp"] as? Long) ?: System.currentTimeMillis(),
                        userId = user.uid,
                        isSynced = true
                    )
                    DataHandler.addNoteFromSync(note)
                }
            }
        }
    }
}