package com.example.noteon

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import android.widget.TextView
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FolderActivity : BaseNavigationActivity() {
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerViewFolders: RecyclerView
    private lateinit var fabAddFolder: FloatingActionButton
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var searchView: SearchView
    override lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar
    private lateinit var authManager: AuthManager
    private lateinit var guestSession: GuestSession

    override val currentNavigationItem: Int = R.id.nav_folders

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folders)

        // Initialize managers
        authManager = AuthManager.getInstance(this)
        guestSession = GuestSession.getInstance(this)

        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupSearchView()
        setupNavigationFooter()
        updateNavigationHeader()

        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(currentNavigationItem)
    }

    private fun setupViews() {
        recyclerViewFolders = findViewById(R.id.recyclerViewFolders)
        fabAddFolder = findViewById(R.id.fabAddFolder)
        searchView = findViewById(R.id.searchViewFolders)
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupRecyclerView() {
        val currentState = authStateManager.getCurrentState()
        val currentUserId = when (currentState) {
            is AuthState.Authenticated -> currentState.user.uid
            is AuthState.Guest -> GuestSession.getInstance(this).getGuestId()
            else -> null
        }

        folderAdapter = FolderAdapter(
            folders = DataHandler.getAllFolders().filter { it.userId == currentUserId },
            onFolderClick = { folder ->
                val intent = MainActivity.createIntent(this, folder.id)
                startActivity(intent)
                finish()
            },
            onFolderOptions = { folder ->
                FolderOptionsDialog(this).show(folder) {
                    folderAdapter.updateFolders(DataHandler.getAllFolders().filter { it.userId == currentUserId })
                }
            }
        )
        recyclerViewFolders.layoutManager = LinearLayoutManager(this)
        recyclerViewFolders.adapter = folderAdapter
    }

    private fun setupFab() {
        fabAddFolder.setOnClickListener {
            showCreateFolderDialog()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setTitle(R.string.folders)
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    // Only search within current user's folders
                    val filteredFolders = DataHandler.searchFolders(newText)
                        .filter { folder -> folder.userId == (if (guestSession.isGuestSession())
                            guestSession.getGuestId()
                        else authManager.currentUser?.uid) }
                    folderAdapter.updateFolders(filteredFolders)
                }
                return true
            }
        })
    }

    private fun showCreateFolderDialog() {
        DialogUtils.showCreateFolderDialog(
            context = this,
            onSave = { name, description ->
                DataHandler.createFolder(name, description, this)
                folderAdapter.updateFolders(DataHandler.getFoldersByUser(this))
            }
        )
    }

    private fun updateNavigationHeader() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)
        val footerContainer = navigationView.findViewById<View>(R.id.nav_footer_container)
        val usernameTextView = headerView.findViewById<TextView>(R.id.nav_header_username)
        val emailTextView = headerView.findViewById<TextView>(R.id.nav_header_email)

        when {
            authManager.currentUser != null -> {
                // User is logged in
                footerContainer?.visibility = View.GONE
                val userName = DataHandler.getUserName(authManager.currentUser!!.uid)
                if (userName != null) {
                    usernameTextView?.text = userName
                } else {
                    usernameTextView?.text = authManager.currentUser!!.email?.substringBefore('@')?.capitalize()
                }
                emailTextView?.text = authManager.currentUser!!.email
            }
            guestSession.isGuestSession() -> {
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
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                syncFolders()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        // Hide sync option for guest users or non-authenticated users
        val isAuthenticated = authManager.currentUser != null
        menu.findItem(R.id.action_sync)?.isVisible = isAuthenticated
        return super.onPrepareOptionsMenu(menu)
    }

    private fun syncFolders() {
        if (guestSession.isGuestSession()) {
            Toast.makeText(this, R.string.sync_not_available_guest, Toast.LENGTH_SHORT).show()
            return
        }

        val progressDialog = DialogUtils.showProgressDialog(
            context = this,
            message = getString(R.string.sync_in_progress)
        )

        lifecycleScope.launch {
            try {
                // Sync folders
                authManager.backupFolders()
                authManager.restoreFolders()

                // Update UI
                folderAdapter.updateFolders(DataHandler.getFoldersByUser(this@FolderActivity))

                Toast.makeText(this@FolderActivity, R.string.notes_synced, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@FolderActivity, R.string.sync_error, Toast.LENGTH_SHORT).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }

}