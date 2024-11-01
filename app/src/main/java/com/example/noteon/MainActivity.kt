package com.example.noteon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var searchView: SearchView
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var notes: List<Note>

    companion object {
        private const val EXTRA_FOLDER_ID = "folder_id"

        fun createIntent(context: Context, folderId: Long): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(EXTRA_FOLDER_ID, folderId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView: NavigationView = findViewById(R.id.navigationView)
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

        navigationView.setNavigationItemSelectedListener(this)

        // Generate dummy notes
        notes = DataHandler.generateDummyNotes(20) // Generate 20 dummy notes

        val folderId = intent.getLongExtra(EXTRA_FOLDER_ID, 0)
        notes = if (folderId == 0L) {
            DataHandler.getAllNotes()
        } else {
            DataHandler.getNotesInFolder(folderId)
        }

        setupRecyclerView()
        setupFab()
        setupSearchView()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            notes = notes,
            onNoteClick = { note -> openNoteDetail(note.id) },
            onMoveNote = { note ->
                MoveNoteDialog(this).show(note.id) { newFolderId ->
                    // Refresh the list after moving
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

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_all_notes -> {
                // Handle all notes action
            }
            R.id.nav_folders -> {
                startActivity(Intent(this, FolderActivity::class.java))
            }
            R.id.nav_favorites -> {
                // Handle favorites action
            }
            R.id.nav_trash -> {
                // Handle trash action
            }
            R.id.nav_settings -> {
                // Handle settings action
            }
            R.id.nav_about -> {
                // Handle about action
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        updateNotesList()
    }

    private fun updateNotesList() {
        notes = DataHandler.getAllNotes()
        notesAdapter.updateNotes(notes)
    }
}