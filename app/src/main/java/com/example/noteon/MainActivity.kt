package com.example.noteon

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerViewNotes: RecyclerView
    private lateinit var navigationView: NavigationView
    private lateinit var fabAddNote: FloatingActionButton
    private lateinit var fabChatbot: FloatingActionButton
    private lateinit var searchView: SearchView
    private var isSearching = false
    private lateinit var searchHelperText: TextView
    private lateinit var searchLoadingIndicator: ProgressBar
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var intelligentSearchService: IntelligentSearchService
    private lateinit var notesAdapter: NotesAdapter
    private lateinit var notes: List<Note>
    private var currentFolderId: Long = 0
    private var currentView = ViewType.ALL_NOTES
    private lateinit var authManager: AuthManager
    private lateinit var guestSession: GuestSession

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

        // Initialize managers
        authStateManager = AuthStateManager.getInstance(this)
        guestSession = GuestSession.getInstance(this)
        preferencesManager = PreferencesManager.getInstance(this)
        intelligentSearchService = IntelligentSearchService()

        setupViews()
        setupToolbar()
        setupDrawer()
        setupFolderHandling()
        setupRecyclerView()
        setupFab()
        setupSearchView()
        setupNavigationFooter()
    }


    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
        recyclerViewNotes = findViewById(R.id.recyclerViewNotes)
        fabAddNote = findViewById(R.id.fabAddNote)
        fabChatbot = findViewById(R.id.fabChatbot)
        searchView = findViewById(R.id.searchView)
        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
    }

    private fun setupDrawer() {
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
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
            coroutineScope = lifecycleScope,
            onNoteClick = { note -> openNoteDetail(note.id) },
            onAIOptions = { note -> AIOptionsDialog(this).show(note) },
            onNoteOptions = { note ->
                NoteOptionsDialog(this, lifecycleScope).show(note) {
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
        fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatbotActivity::class.java))
        }
    }

    private fun setupSearchView() {
        searchHelperText = TextView(this).apply {
            id = View.generateViewId()
            setTextAppearance(android.R.style.TextAppearance_Small)
            alpha = 0.7f
            visibility = View.GONE
        }

        searchLoadingIndicator = ProgressBar(this, null, android.R.attr.progressBarStyleSmall).apply {
            id = View.generateViewId()
            visibility = View.GONE
        }

        // Add helper text and loading indicator below SearchView
        val searchContainer = searchView.parent as ViewGroup
        val searchIndex = searchContainer.indexOfChild(searchView)

        // Layout for loading indicator and helper text
        val horizontalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 4, 16, 16)
            }

            addView(searchLoadingIndicator, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8
            })
            addView(searchHelperText, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        searchContainer.addView(horizontalLayout, searchIndex + 1)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!query.isNullOrBlank()) {
                    performSearch(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    notesAdapter.updateNotes(notes)
                    updateSearchHelperText(false)
                    return true
                }

                if (preferencesManager.isIntelligentSearchEnabled()) {
                    updateSearchHelperText(true)
                    filterNotes(newText)
                } else {
                    updateSearchHelperText(false)
                    filterNotes(newText)
                }
                return true
            }
        })
    }

    private fun updateSearchHelperText(showIntelligentSearch: Boolean) {
        if (showIntelligentSearch) {
            searchHelperText.apply {
                text = getString(R.string.press_enter_for_intelligent_search)
                visibility = View.VISIBLE
            }
        } else {
            searchHelperText.visibility = View.GONE
        }
    }

    private fun performSearch(query: String) {
        if (!preferencesManager.isIntelligentSearchEnabled()) {
            filterNotes(query)
            return
        }

        isSearching = true
        searchLoadingIndicator.visibility = View.VISIBLE
        searchHelperText.apply {
            text = getString(R.string.intelligent_search_progress)
            visibility = View.VISIBLE
        }

        lifecycleScope.launch {
            try {
                val searchResults = intelligentSearchService.searchNotes(query, notes)
                notesAdapter.updateNotes(searchResults)
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    R.string.search_error,
                    Toast.LENGTH_SHORT
                ).show()
                filterNotes(query)
            } finally {
                searchLoadingIndicator.visibility = View.GONE
                searchHelperText.visibility = View.GONE
                isSearching = false
            }
        }
    }

    private fun filterNotes(query: String) {
        val baseNotes = when (currentView) {
            ViewType.ALL_NOTES -> DataHandler.getActiveNotes()
            ViewType.FAVORITES -> DataHandler.getFavoriteNotes()
            ViewType.FOLDER -> DataHandler.getNotesInFolder(currentFolderId)
            ViewType.TRASH -> DataHandler.getTrashNotes()
        }

        val filteredNotes = baseNotes.filter { note ->
            note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
        }
        notesAdapter.updateNotes(filteredNotes)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isAuthenticated = authStateManager.getCurrentState() is AuthState.Authenticated
        menu.findItem(R.id.action_sync)?.isVisible = isAuthenticated
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                syncNotes()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun syncNotes() {
        if (authStateManager.getCurrentState() !is AuthState.Authenticated) {
            Toast.makeText(this, R.string.sync_not_available_guest, Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = DialogUtils.showProgressDialog(
            context = this,
            message = getString(R.string.sync_in_progress)
        )

        lifecycleScope.launch {
            try {
                val state = authStateManager.getCurrentState()
                if (state is AuthState.Authenticated) {
                    // Get the Firebase user from the authenticated state
                    val user = state.user
                    DialogUtils.updateProgressDialog(
                        dialog = progressDialog,
                        current = 0,
                        total = 100,
                        message = "Syncing folders..."
                    )
                    user.let {
                        AuthManager.getInstance(this@MainActivity).apply {
                            backupFolders()
                            restoreFolders()

                            backupNotes { current, total ->
                                DialogUtils.updateProgressDialog(
                                    dialog = progressDialog,
                                    current = current,
                                    total = total,
                                    message = getString(R.string.sync_in_progress)
                                )
                            }
                        }
                    }

                    Toast.makeText(this@MainActivity, R.string.notes_synced, Toast.LENGTH_SHORT).show()
                }
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
            ViewType.ALL_NOTES -> DataHandler.getAllNotes().filter { it.isNormal() }
            ViewType.FAVORITES -> DataHandler.getAllNotes().filter { it.isFavorite() }
            ViewType.FOLDER -> DataHandler.getNotesInFolder(currentFolderId)
            ViewType.TRASH -> DataHandler.getAllNotes().filter { it.isTrashed() }
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