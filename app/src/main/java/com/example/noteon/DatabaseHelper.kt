package com.example.noteon

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.Cursor
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "noteon.db"
        private const val DATABASE_VERSION = 4  // Incrementing version for migration

        // Table Names
        private const val TABLE_NOTES = "notes"
        private const val TABLE_FOLDERS = "folders"

        // Common column names
        private const val KEY_ID = "id"
        private const val KEY_TIMESTAMP = "timestamp"

        // NOTES Table - column names
        private const val KEY_TITLE = "title"
        private const val KEY_CONTENT = "content"
        private const val KEY_FOLDER_ID = "folder_id"
        private const val KEY_STATE = "state"  // New column for note state
        private const val KEY_DELETED_DATE = "deleted_date"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SYNC_STATUS = "sync_status"  // New column for sync status

        // FOLDERS Table - column names
        private const val KEY_NAME = "name"
        private const val KEY_DESCRIPTION = "description"

        // Create table statements
        private const val CREATE_TABLE_NOTES = """
            CREATE TABLE $TABLE_NOTES (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_TITLE TEXT NOT NULL,
                $KEY_CONTENT TEXT NOT NULL,
                $KEY_FOLDER_ID INTEGER DEFAULT 0,
                $KEY_STATE INTEGER DEFAULT 0,
                $KEY_DELETED_DATE INTEGER,
                $KEY_TIMESTAMP INTEGER NOT NULL,
                $KEY_USER_ID TEXT,
                $KEY_SYNC_STATUS INTEGER DEFAULT 0
            )
        """

        private const val CREATE_TABLE_FOLDERS = """
            CREATE TABLE $TABLE_FOLDERS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_NAME TEXT NOT NULL,
                $KEY_DESCRIPTION TEXT,
                $KEY_TIMESTAMP INTEGER NOT NULL,
                $KEY_USER_ID TEXT,
                $KEY_SYNC_STATUS INTEGER DEFAULT 0
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_NOTES)
        db.execSQL(CREATE_TABLE_FOLDERS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            // Add new state and sync status columns and migrate existing data
            migrateToNewNoteStructure(db)
        }
    }

    private fun migrateToNewNoteStructure(db: SQLiteDatabase) {
        // Add new columns to notes
        db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $KEY_STATE INTEGER DEFAULT 0")
        db.execSQL("ALTER TABLE $TABLE_NOTES ADD COLUMN $KEY_SYNC_STATUS INTEGER DEFAULT 0")

        // Migrate notes state data
        db.execSQL("""
        UPDATE $TABLE_NOTES SET 
        $KEY_STATE = CASE 
            WHEN is_deleted = 1 THEN 2
            WHEN is_favorite = 1 THEN 1
            ELSE 0
        END
    """)

        // Migrate notes sync status
        db.execSQL("""
        UPDATE $TABLE_NOTES SET 
        $KEY_SYNC_STATUS = CASE 
            WHEN is_synced = 1 THEN 1
            ELSE 0
        END
    """)

        // Add sync status to folders table
        db.execSQL("ALTER TABLE $TABLE_FOLDERS ADD COLUMN $KEY_SYNC_STATUS INTEGER DEFAULT 0")

        // Migrate folders sync status
        db.execSQL("""
        UPDATE $TABLE_FOLDERS SET 
        $KEY_SYNC_STATUS = CASE 
            WHEN is_synced = 1 THEN 1
            ELSE 0
        END
    """)
    }

    private fun getSyncStatusValue(status: SyncStatus): Int {
        return when (status) {
            is SyncStatus.Synced -> 1
            is SyncStatus.NotSynced -> 0
            is SyncStatus.SyncError -> 2
        }
    }

    private fun getSyncStatusFromValue(value: Int): SyncStatus {
        return when (value) {
            1 -> SyncStatus.Synced
            2 -> SyncStatus.SyncError("Unknown error")
            else -> SyncStatus.NotSynced
        }
    }

    fun addNote(note: Note): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            if (note.id != 0L) {
                put(KEY_ID, note.id)
            }
            put(KEY_TITLE, note.title)
            put(KEY_CONTENT, note.content)
            put(KEY_FOLDER_ID, note.metadata.folderId)
            put(KEY_STATE, NoteState.toDatabaseValue(note.state))
            put(KEY_DELETED_DATE, if (note.state is NoteState.Trash) note.state.deletedDate else null)
            put(KEY_TIMESTAMP, note.timestamp)
            put(KEY_USER_ID, note.metadata.userId)
            put(KEY_SYNC_STATUS, getSyncStatusValue(note.metadata.syncStatus))
        }

        return db.insertWithOnConflict(
            TABLE_NOTES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.query(
                TABLE_NOTES,
                null,
                null,
                null,
                null, null,
                "$KEY_TIMESTAMP DESC"
            )

            if (cursor?.moveToFirst() == true) {
                do {
                    notes.add(createNoteFromCursor(cursor))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return notes
    }

    private fun createNoteFromCursor(cursor: Cursor): Note {
        val state = NoteState.fromDatabaseValue(
            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_STATE)),
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DELETED_DATE))
        )

        val metadata = NoteMetadata(
            folderId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FOLDER_ID)),
            userId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
            syncStatus = getSyncStatusFromValue(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SYNC_STATUS))
            )
        )

        return Note(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
            content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT)),
            state = state,
            metadata = metadata,
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
        )
    }

    fun getNoteById(id: Long): Note? {
        val db = this.readableDatabase
        var cursor: Cursor? = null
        return try {
            cursor = db.query(
                TABLE_NOTES,
                null,
                "$KEY_ID = ?",
                arrayOf(id.toString()),
                null, null, null
            )

            if (cursor?.moveToFirst() == true) {
                createNoteFromCursor(cursor)
            } else null
        } finally {
            cursor?.close()
        }
    }

    fun updateNote(note: Note): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, note.title)
            put(KEY_CONTENT, note.content)
            put(KEY_FOLDER_ID, note.metadata.folderId)
            put(KEY_STATE, NoteState.toDatabaseValue(note.state))
            put(KEY_DELETED_DATE, if (note.state is NoteState.Trash) note.state.deletedDate else null)
            put(KEY_TIMESTAMP, note.timestamp)
            put(KEY_USER_ID, note.metadata.userId)
            put(KEY_SYNC_STATUS, getSyncStatusValue(note.metadata.syncStatus))
        }
        return db.update(TABLE_NOTES, values, "$KEY_ID = ?", arrayOf(note.id.toString()))
    }

    fun getNotesInState(state: NoteState): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.query(
                TABLE_NOTES,
                null,
                "$KEY_STATE = ?",
                arrayOf(NoteState.toDatabaseValue(state).toString()),
                null, null,
                when (state) {
                    is NoteState.Trash -> "$KEY_DELETED_DATE DESC"
                    else -> "$KEY_TIMESTAMP DESC"
                }
            )

            if (cursor?.moveToFirst() == true) {
                do {
                    notes.add(createNoteFromCursor(cursor))
                } while (cursor.moveToNext())
            }
        } finally {
            cursor?.close()
        }
        return notes
    }

    // Convenience methods
    fun getFavoriteNotes(): List<Note> = getNotesInState(NoteState.Favorite)
    fun getTrashNotes(): List<Note> = getNotesInState(NoteState.Trash())
    fun getActiveNotes(): List<Note> = getNotesInState(NoteState.Active)

    fun deleteNote(noteId: Long): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_NOTES, "$KEY_ID = ?", arrayOf(noteId.toString()))
    }

    fun getNotesInFolder(folderId: Long): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$KEY_FOLDER_ID = ? AND $KEY_STATE != ?",
            arrayOf(folderId.toString(), NoteState.toDatabaseValue(NoteState.Trash()).toString()),
            null, null,
            "$KEY_TIMESTAMP DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                notes.add(createNoteFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return notes
    }

    // Folder CRUD Operations
    fun addFolder(folder: Folder): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, folder.name)
            put(KEY_DESCRIPTION, folder.description)
            put(KEY_TIMESTAMP, folder.timestamp)
            put(KEY_USER_ID, folder.userId)
            put(KEY_SYNC_STATUS, getSyncStatusValue(folder.metadata.syncStatus))
        }
        return db.insert(TABLE_FOLDERS, null, values)
    }

    fun updateFolder(folder: Folder): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, folder.name)
            put(KEY_DESCRIPTION, folder.description)
            put(KEY_USER_ID, folder.userId)
            put(KEY_SYNC_STATUS, getSyncStatusValue(folder.metadata.syncStatus))
        }
        return db.update(TABLE_FOLDERS, values, "$KEY_ID = ?", arrayOf(folder.id.toString()))
    }

    private fun createFolderFromCursor(cursor: Cursor): Folder {
        val metadata = FolderMetadata(
            userId = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
            syncStatus = getSyncStatusFromValue(
                cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SYNC_STATUS))
            )
        )

        return Folder(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
            description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP)),
            metadata = metadata
        )
    }

    fun getFolderById(id: Long): Folder? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_FOLDERS,
            null,
            "$KEY_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            createFolderFromCursor(cursor)
        } else null.also { cursor.close() }
    }

    fun getFoldersByUser(userId: String): List<Folder> {
        val folders = mutableListOf<Folder>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_FOLDERS,
            null,
            "$KEY_USER_ID = ?",
            arrayOf(userId),
            null,
            null,
            "$KEY_NAME ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                folders.add(createFolderFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return folders
    }

    fun getAllFolders(): List<Folder> {
        val folders = mutableListOf<Folder>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_FOLDERS,
            null,
            null,
            null,
            null,
            null,
            "$KEY_NAME ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                folders.add(createFolderFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return folders
    }

    fun deleteFolder(folderId: Long): Int {
        val db = this.writableDatabase
        // First, update all notes in this folder to have no folder (folderId = 0)
        val values = ContentValues().apply {
            put(KEY_FOLDER_ID, 0)
        }
        db.update(TABLE_NOTES, values, "$KEY_FOLDER_ID = ?", arrayOf(folderId.toString()))

        // Then delete the folder
        return db.delete(TABLE_FOLDERS, "$KEY_ID = ?", arrayOf(folderId.toString()))
    }

    fun searchFolders(query: String): List<Folder> {
        val folders = mutableListOf<Folder>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_FOLDERS,
            null,
            "$KEY_NAME LIKE ? OR $KEY_DESCRIPTION LIKE ?",
            arrayOf("%$query%", "%$query%"),
            null, null,
            "$KEY_NAME ASC"
        )

        if (cursor.moveToFirst()) {
            do {
                folders.add(createFolderFromCursor(cursor))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return folders
    }

    fun upsertSyncedFolder(folder: Folder): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(KEY_ID, folder.id)
            put(KEY_NAME, folder.name)
            put(KEY_DESCRIPTION, folder.description)
            put(KEY_TIMESTAMP, folder.timestamp)
            put(KEY_USER_ID, folder.userId)
            put(KEY_SYNC_STATUS, getSyncStatusValue(SyncStatus.Synced))
        }

        return db.insertWithOnConflict(
            TABLE_FOLDERS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun upsertSyncedNote(note: Note): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ID, note.id)
            put(KEY_TITLE, note.title)
            put(KEY_CONTENT, note.content)
            put(KEY_FOLDER_ID, note.metadata.folderId)
            put(KEY_STATE, NoteState.toDatabaseValue(note.state))
            put(KEY_DELETED_DATE, if (note.state is NoteState.Trash) note.state.deletedDate else null)
            put(KEY_TIMESTAMP, note.timestamp)
            put(KEY_SYNC_STATUS, getSyncStatusValue(SyncStatus.Synced))
            put(KEY_USER_ID, note.metadata.userId)
        }

        return db.insertWithOnConflict(
            TABLE_NOTES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun emptyTrash() {
        val db = this.writableDatabase
        db.delete(TABLE_NOTES, "$KEY_STATE = ?",
            arrayOf(NoteState.toDatabaseValue(NoteState.Trash()).toString()))
    }
}