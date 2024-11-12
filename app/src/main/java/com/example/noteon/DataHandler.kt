package com.example.noteon

import kotlin.random.Random

object DataHandler {
    private val notes = mutableListOf<Note>()
    private val folders = mutableListOf<Folder>()
    private var lastNoteId = 0L
    private var lastFolderId = 0L

    private val dummyTitles = listOf(
        "Grocery List", "Meeting Notes", "Book Ideas", "Travel Plans",
        "Fitness Goals", "Recipe", "Movie Recommendations", "Birthday Gift Ideas",
        "Project Brainstorm", "Quotes", "Bucket List", "Homework Assignments"
    )

    private val dummyContents = listOf(
        "Remember to buy milk, eggs, and bread.",
        "Discuss Q3 goals and review last month's performance.",
        "A mystery novel set in a small town with a twist ending.",
        "Research hotels in Paris and book flights for next month.",
        "Run 5k three times a week and do strength training on weekends.",
        "Ingredients: flour, sugar, eggs. Instructions: Mix and bake at 350°F.",
        "1. The Shawshank Redemption\n2. Inception\n3. The Matrix",
        "Mom: scarf, Dad: book, Sister: headphones",
        "New app idea: AI-powered plant care assistant",
        "\"The only way to do great work is to love what you do.\" - Steve Jobs",
        "1. Skydiving\n2. Learn a new language\n3. Visit all 7 continents",
        "Math: pg 45-47, Science: lab report, English: essay outline"
    )

    init {
        if (notes.isEmpty()) {
            generateDummyNotes(20)
        }
    }

    private fun generateDummyNotes(count: Int): List<Note> {
        notes.clear()
        repeat(count) {
            val title = dummyTitles[Random.nextInt(dummyTitles.size)]
            val content = dummyContents[Random.nextInt(dummyContents.size)]
            val timestamp = System.currentTimeMillis() - Random.nextLong(0, 30L * 24 * 60 * 60 * 1000)
            val isFavorite = Random.nextBoolean()
            val isDeleted = Random.nextDouble() < 0.1 // 10% chance of being in trash
            val deletedDate = if (isDeleted) System.currentTimeMillis() - Random.nextLong(0, 7L * 24 * 60 * 60 * 1000) else null

            notes.add(Note(
                id = ++lastNoteId,
                title = title,
                content = content,
                timestamp = timestamp,
                isFavorite = isFavorite,
                isDeleted = isDeleted,
                deletedDate = deletedDate
            ))
        }
        return getAllNotes() // Return only non-deleted notes
    }

    fun createFolder(name: String, description: String): Folder {
        val folder = Folder(++lastFolderId, name, description)
        folders.add(folder)
        return folder
    }

    fun searchFolders(query: String): List<Folder> {
        return folders.filter { folder ->
            folder.name.contains(query, ignoreCase = true) ||
                    folder.description.contains(query, ignoreCase = true)
        }
    }

    fun getAllFolders(): List<Folder> = folders.toList()

    fun getFolderById(id: Long): Folder? = folders.find { it.id == id }

    fun updateFolder(folderId: Long, newName: String, newDescription: String) {
        folders.find { it.id == folderId }?.let {
            it.name = newName
            it.description = newDescription
        }
    }

    fun deleteFolder(folderId: Long) {
        folders.removeIf { it.id == folderId }
        // Move notes in deleted folder to root (no folder)
        notes.filter { it.folderId == folderId }.forEach { it.folderId = 0 }
    }

    fun moveNoteToFolder(noteId: Long, folderId: Long) {
        notes.find { it.id == noteId }?.apply {
            this.folderId = folderId
        }
    }

    fun addNote(title: String, content: String, folderId: Long = 0): Note {
        val newNote = Note(++lastNoteId, title, content, folderId)
        notes.add(newNote)
        return newNote
    }

    fun getAllNotes(): List<Note> = notes.filter { !it.isDeleted }.sortedByDescending { it.timestamp }

    fun getNoteById(id: Long): Note? = notes.find { it.id == id }

    fun updateNote(updatedNote: Note) {
        val index = notes.indexOfFirst { it.id == updatedNote.id }
        if (index != -1) {
            notes[index] = updatedNote
        }
    }

    fun toggleNoteFavorite(noteId: Long) {
        notes.find { it.id == noteId }?.let { note ->
            note.isFavorite = !note.isFavorite
        }
    }

    fun getFavoriteNotes(): List<Note> =
        notes.filter { it.isFavorite && !it.isDeleted }.sortedByDescending { it.timestamp }

    fun getNotesInFolder(folderId: Long): List<Note> =
        notes.filter { it.folderId == folderId && !it.isDeleted }.sortedByDescending { it.timestamp }

    fun moveNoteToTrash(noteId: Long) {
        notes.find { it.id == noteId }?.apply {
            isDeleted = true
            deletedDate = System.currentTimeMillis()
        }
    }

    fun restoreNoteFromTrash(noteId: Long) {
        notes.find { it.id == noteId }?.apply {
            isDeleted = false
            deletedDate = null
        }
    }

    fun getTrashNotes(): List<Note> =
        notes.filter { it.isDeleted }.sortedByDescending { it.deletedDate }

    fun deleteNotePermanently(noteId: Long) {
        notes.removeIf { it.id == noteId }
    }

    fun emptyTrash() {
        notes.removeIf { it.isDeleted }
    }
}

