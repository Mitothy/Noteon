package com.example.noteon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : BaseNavigationActivity() {

    override lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var navigationView: NavigationView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var notes: List<Note>
    private var currentFolderId: Long = 0
    private var currentView = ViewType.ALL_NOTES

    override val currentNavigationItem: Int
        get() = when (currentView) {
            ViewType.ALL_NOTES -> R.id.nav_all_notes
            ViewType.FAVORITES -> R.id.nav_favorites
            ViewType.FOLDER -> R.id.nav_folders
            ViewType.TRASH -> R.id.nav_trash
        }

    enum class ViewType {
        ALL_NOTES,
        FAVORITES,
        FOLDER,
        TRASH
    }

    companion object {
        private const val EXTRA_FOLDER_ID = "folder_id"
        private const val EXTRA_VIEW_TYPE = "view_type"

        fun createIntent(context: Context, folderId: Long): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FOLDER_ID, folderId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)
        fabAddNote = findViewById(R.id.fabAddNote)
        searchView = findViewById(R.id.searchView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)

        val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, 0)
        notes = if (folderId == 0L) {
            DataHandler.getAllNotes()
        } else {
            DataHandler.getNotesInFolder(folderId)
        }

        setupRecyclerView()
        setupFab()
        setupSearchView()

        currentFolderId = intent.getLongExtra(EXTRA_FOLDER_ID, 0)
        intent.getStringExtra(EXTRA_VIEW_TYPE)?.let { viewTypeName ->
            currentView = try {
                ViewType.valueOf(viewTypeName)
            } catch (e: IllegalArgumentException) {
                if (currentFolderId == 0L) ViewType.ALL_NOTES else ViewType.FOLDER
            }
        } ?: run {
            currentView = if (currentFolderId == 0L) ViewType.ALL_NOTES else ViewType.FOLDER
        }

        updateNotesList()
        updateTitle()
        updateNavigationSelection()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentFolderId = intent.getLongExtra(EXTRA_FOLDER_ID, 0)
        intent.getStringExtra(EXTRA_VIEW_TYPE)?.let { viewTypeName ->
            currentView = try {
                ViewType.valueOf(viewTypeName)
            } catch (e: IllegalArgumentException) {
                if (currentFolderId == 0L) ViewType.ALL_NOTES else ViewType.FOLDER
            }
        } ?: run {
            currentView = if (currentFolderId == 0L) ViewType.ALL_NOTES else ViewType.FOLDER
        }
        updateNotesList()
        updateTitle()
        updateNavigationSelection()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            notes = notes,
            onNoteClick = { note -> openNoteDetail(note.id) },
            onNoteOptions = { note ->
                NoteOptionsDialog(this).show(
                    note = note,
                    currentFolderId = currentFolderId,
                    isTrashView = currentView == ViewType.TRASH
                ) {
                    updateNotesList()
                }
            }
        )
        recyclerViewNotes.layoutManager = LinearLayoutManager(this)
        recyclerViewNotes.adapter = notesAdapter
    }

    private fun setupFab() {
        fabAddNote.setOnClickListener {
            val intent = Intent(this, AddNoteActivity::class.java)
            intent.putExtra("folder_id", currentFolderId)
            startActivity(intent)
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterNotes(newText)
                return true
            }
        })
    }

    private fun filterNotes(query: String?) {
        if (query.isNullOrBlank()) {
            notesAdapter.updateNotes(notes)
        } else {
            val filteredNotes = notes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                        note.content.contains(query, ignoreCase = true)
            }
            notesAdapter.updateNotes(filteredNotes)
        }
    }

    private fun openNoteDetail(noteId: Long) {
        val intent = Intent(this, NoteDetailActivity::class.java)
        intent.putExtra("NOTE_ID", noteId)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateNotesList()
    }

    override fun onNavigationChanged(itemId: Int) {
        when (itemId) {
            R.id.nav_all_notes -> {
                currentView = ViewType.ALL_NOTES
                currentFolderId = 0
            }
            R.id.nav_favorites -> {
                currentView = ViewType.FAVORITES
                currentFolderId = 0
            }
            R.id.nav_trash -> {
                currentView = ViewType.TRASH
                currentFolderId = 0
            }
        }
        updateNotesList()
        updateTitle()
        updateNavigationSelection()
    }

    private fun updateNavigationSelection() {
        val menuItemId = when (currentView) {
            ViewType.ALL_NOTES -> R.id.nav_all_notes
            ViewType.FAVORITES -> R.id.nav_favorites
            ViewType.FOLDER -> R.id.nav_folders
            ViewType.TRASH -> R.id.nav_trash
        }
        navigationView.setCheckedItem(menuItemId)
    }


    private fun updateNotesList() {
        notes = when (currentView) {
            ViewType.ALL_NOTES -> DataHandler.getAllNotes()
            ViewType.FAVORITES -> DataHandler.getFavoriteNotes()
            ViewType.FOLDER -> DataHandler.getNotesInFolder(currentFolderId)
            ViewType.TRASH -> DataHandler.getTrashNotes()
        }
        notesAdapter.updateNotes(notes)
    }

    private fun updateTitle() {
        supportActionBar?.title = when (currentView) {
            ViewType.ALL_NOTES -> getString(R.string.all_notes)
            ViewType.FAVORITES -> getString(R.string.favorites)
            ViewType.FOLDER -> DataHandler.getFolderById(currentFolderId)?.name
                ?: getString(R.string.all_notes)
            ViewType.TRASH -> getString(R.string.trash)
        }
    }
}