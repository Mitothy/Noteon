package com.example.noteon

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "noteon.db"
        private const val DATABASE_VERSION = 1

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
        private const val KEY_IS_FAVORITE = "is_favorite"
        private const val KEY_IS_DELETED = "is_deleted"
        private const val KEY_DELETED_DATE = "deleted_date"

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
                $KEY_IS_FAVORITE INTEGER DEFAULT 0,
                $KEY_IS_DELETED INTEGER DEFAULT 0,
                $KEY_DELETED_DATE INTEGER,
                $KEY_TIMESTAMP INTEGER NOT NULL
            )
        """

        private const val CREATE_TABLE_FOLDERS = """
            CREATE TABLE $TABLE_FOLDERS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_NAME TEXT NOT NULL,
                $KEY_DESCRIPTION TEXT,
                $KEY_TIMESTAMP INTEGER NOT NULL
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_NOTES)
        db.execSQL(CREATE_TABLE_FOLDERS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older tables if existed
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOLDERS")
        // Create tables again
        onCreate(db)
    }

    // Note CRUD Operations
    fun addNote(note: Note): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, note.title)
            put(KEY_CONTENT, note.content)
            put(KEY_FOLDER_ID, note.folderId)
            put(KEY_IS_FAVORITE, if (note.isFavorite) 1 else 0)
            put(KEY_IS_DELETED, if (note.isDeleted) 1 else 0)
            put(KEY_DELETED_DATE, note.deletedDate)
            put(KEY_TIMESTAMP, note.timestamp)
        }
        return db.insert(TABLE_NOTES, null, values)
    }

    fun getNoteById(id: Long): Note? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$KEY_ID = ?",
            arrayOf(id.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            Note(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT)),
                folderId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FOLDER_ID)),
                isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_FAVORITE)) == 1,
                isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_DELETED)) == 1,
                deletedDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DELETED_DATE)),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
            )
        } else null.also { cursor.close() }
    }

    fun getAllNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$KEY_IS_DELETED = 0",
            null,
            null, null,
            "$KEY_TIMESTAMP DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                notes.add(Note(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT)),
                    folderId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FOLDER_ID)),
                    isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_FAVORITE)) == 1,
                    isDeleted = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_DELETED)) == 1,
                    deletedDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DELETED_DATE)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return notes
    }

    fun updateNote(note: Note): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_TITLE, note.title)
            put(KEY_CONTENT, note.content)
            put(KEY_FOLDER_ID, note.folderId)
            put(KEY_IS_FAVORITE, if (note.isFavorite) 1 else 0)
            put(KEY_IS_DELETED, if (note.isDeleted) 1 else 0)
            put(KEY_DELETED_DATE, note.deletedDate)
        }
        return db.update(TABLE_NOTES, values, "$KEY_ID = ?", arrayOf(note.id.toString()))
    }

    fun deleteNote(noteId: Long): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_NOTES, "$KEY_ID = ?", arrayOf(noteId.toString()))
    }

    fun getFavoriteNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$KEY_IS_FAVORITE = 1 AND $KEY_IS_DELETED = 0",
            null,
            null, null,
            "$KEY_TIMESTAMP DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                notes.add(Note(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT)),
                    folderId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FOLDER_ID)),
                    isFavorite = true,
                    isDeleted = false,
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return notes
    }

    fun getTrashNotes(): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$KEY_IS_DELETED = 1",
            null,
            null, null,
            "$KEY_DELETED_DATE DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                notes.add(Note(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT)),
                    folderId = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_FOLDER_ID)),
                    isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_FAVORITE)) == 1,
                    isDeleted = true,
                    deletedDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DELETED_DATE)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return notes
    }

    fun getNotesInFolder(folderId: Long): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NOTES,
            null,
            "$KEY_FOLDER_ID = ? AND $KEY_IS_DELETED = 0",
            arrayOf(folderId.toString()),
            null, null,
            "$KEY_TIMESTAMP DESC"
        )

        if (cursor.moveToFirst()) {
            do {
                notes.add(Note(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(KEY_TITLE)),
                    content = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTENT)),
                    folderId = folderId,
                    isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_FAVORITE)) == 1,
                    isDeleted = false,
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                ))
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
        }
        return db.insert(TABLE_FOLDERS, null, values)
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
            Folder(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
            )
        } else null.also { cursor.close() }
    }

    fun getAllFolders(): List<Folder> {
        val folders = mutableListOf<Folder>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_FOLDERS, null, null, null, null, null, "$KEY_NAME ASC")

        if (cursor.moveToFirst()) {
            do {
                folders.add(Folder(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return folders
    }

    fun updateFolder(folder: Folder): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_NAME, folder.name)
            put(KEY_DESCRIPTION, folder.description)
        }
        return db.update(TABLE_FOLDERS, values, "$KEY_ID = ?", arrayOf(folder.id.toString()))
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
                folders.add(Folder(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_TIMESTAMP))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return folders
    }

    fun emptyTrash() {
        val db = this.writableDatabase
        db.delete(TABLE_NOTES, "$KEY_IS_DELETED = 1", null)
    }
}