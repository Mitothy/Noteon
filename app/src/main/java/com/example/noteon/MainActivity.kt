package com.example.noteon

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
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
    private lateinit var authManager: AuthManager
    private lateinit var guestSession: GuestSession  // Add this

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
        guestSession = GuestSession.getInstance(this)

        // Check if user is signed in or in guest mode
        if (authManager.currentUser == null && !GuestSession.getInstance(this).isGuestSession()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        setupViews()
        setupDrawer(toolbar)
        setupFolderHandling()
        setupRecyclerView()
        setupFab()
        setupSearchView()
        setupNavigationFooter()
        updateNavigationHeader()
    }

    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)
        fabAddNote = findViewById(R.id.fabAddNote)
        fabChatbot = findViewById(R.id.fabChatbot)
        searchView = findViewById(R.id.searchView)
        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setupDrawer(toolbar: Toolbar) {
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupFolderHandling() {
        currentFolderId = intent.getLongExtra(EXTRA_FOLDER_ID, 0)
        notes = if (currentFolderId == 0L) {
            DataHandler.getAllNotes()
        } else {
            DataHandler.getNotesInFolder(currentFolderId)
        }

        intent.getStringExtra(EXTRA_VIEW_TYPE)?.let { viewTypeName ->
            currentView = try {
                ViewType.valueOf(viewTypeName)
            } catch (e: IllegalArgumentException) {
                if (currentFolderId == 0L) ViewType.ALL_NOTES else ViewType.FOLDER
            }
        } ?: run {
            currentView = if (currentFolderId == 0L) ViewType.ALL_NOTES else ViewType.FOLDER
        }

        updateTitle()
        updateNavigationSelection()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            notes = notes,
            onNoteClick = { note -> openNoteDetail(note.id) },
            coroutineScope = lifecycleScope,
            onAIOptions = { note -> AIOptionsDialog(this).show(note) },
            isTrashView = currentView == ViewType.TRASH
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Hide sync option for guest users or non-authenticated users
        val isAuthenticated = authManager.currentUser != null
        menu.findItem(R.id.action_sync)?.isVisible = isAuthenticated

        // Set correct sign out text
        menu.findItem(R.id.action_sign_out)?.title =
            if (guestSession.isGuestSession())
                getString(R.string.exit_guest_mode)
            else if (isAuthenticated)
                getString(R.string.sign_out)
            else
                getString(R.string.login)

        return super.onPrepareOptionsMenu(menu)
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
        if (guestSession.isGuestSession()) {
            Toast.makeText(this, R.string.sync_not_available_guest, Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = Dialog(this).apply {
            setContentView(R.layout.dialog_progress)
            setCancelable(false)
        }

        val progressBar = progressDialog.findViewById<ProgressBar>(R.id.progressBar)
        val textViewPercentage = progressDialog.findViewById<TextView>(R.id.textViewPercentage)
        val textViewProgress = progressDialog.findViewById<TextView>(R.id.textViewProgress)

        lifecycleScope.launch {
            try {
                progressDialog.show()

                // First sync folders
                textViewProgress.text = "Syncing folders..."
                authManager.backupFolders()
                authManager.restoreFolders()

                // Then sync notes
                textViewProgress.text = getString(R.string.sync_in_progress)
                authManager.backupNotes { current, total ->
                    progressBar.max = total
                    progressBar.progress = current
                    val percentage = ((current.toFloat() / total) * 100).toInt()
                    textViewPercentage.text = "$percentage%"
                }

                Toast.makeText(this@MainActivity, R.string.notes_synced, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.sync_error, Toast.LENGTH_SHORT).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun signOut() {
        if (guestSession.isGuestSession()) {
            // Show confirmation dialog for guest mode exit
            AlertDialog.Builder(this)
                .setTitle(R.string.exit_guest_mode)
                .setMessage(R.string.exit_guest_mode_message)
                .setPositiveButton(R.string.exit) { _, _ ->
                    guestSession.clearGuestData(this)
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else if (authManager.currentUser != null) {
            // Regular sign out for authenticated users
            authManager.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // If neither guest nor authenticated, go to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
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

    private fun updateNavigationHeader() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)
        val footerContainer = navigationView.findViewById<View>(R.id.nav_footer_container)
        val usernameTextView = headerView.findViewById<TextView>(R.id.nav_header_username)
        val emailTextView = headerView.findViewById<TextView>(R.id.nav_header_email)

        when {
            authManager.currentUser != null -> {
                // User is logged in - existing logic
                footerContainer?.visibility = View.GONE
                val userName = DataHandler.getUserName(authManager.currentUser!!.uid)
                if (userName != null) {
                    usernameTextView?.text = userName
                } else {
                    usernameTextView?.text = authManager.currentUser!!.email?.substringBefore('@')?.capitalize()
                }
                emailTextView?.text = authManager.currentUser!!.email
            }
            GuestSession.getInstance(this).isGuestSession() -> {
                // User is in guest mode
                footerContainer?.visibility = View.VISIBLE
                usernameTextView?.text = getString(R.string.app_name)
                emailTextView?.text = getString(R.string.guest_user)
            }
            else -> {
                // Not logged in and not in guest mode
                footerContainer?.visibility = View.VISIBLE
                usernameTextView?.text = getString(R.string.app_name)
                emailTextView?.text = getString(R.string.guest_user)
            }
        }
    }
}