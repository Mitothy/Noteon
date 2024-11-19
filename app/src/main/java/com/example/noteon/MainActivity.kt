package com.example.noteon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch

class MainActivity : BaseNavigationActivity() {

    override lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var navigationView: NavigationView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var fabChatbot: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var notes: List<Note>
    private var currentFolderId: Long = 0
    private var currentView = ViewType.ALL_NOTES
    private lateinit var authManager: AuthManager  // Add this

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

        // Initialize AuthManager
        authManager = AuthManager.getInstance(this)

        // Check if user is signed in
        if (authManager.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)
        fabAddNote = findViewById(R.id.fabAddNote)
        fabChatbot = findViewById(R.id.fabChatbot)
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
        setupNavigationFooter()

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu resource
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                syncNotes()
                true
            }
            R.id.action_sign_out -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun syncNotes() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, R.string.sync_in_progress, Toast.LENGTH_SHORT).show()
                authManager.backupNotes()
                Toast.makeText(this@MainActivity, R.string.notes_synced, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.sync_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signOut() {
        authManager.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
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
                    isTrashView = currentView == ViewType.TRASH
                ) {
                    updateNotesList()
                }
            },
            onAIOptions = { note ->
                AIOptionsDialog(this).show(note)
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
        fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
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