package com.example.noteon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class FolderActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var navigationView: NavigationView
    private lateinit var recyclerViewFolders: RecyclerView
    private lateinit var fabAddFolder: FloatingActionButton
    private lateinit var folderAdapter: FolderAdapter
    private lateinit var searchView: SearchView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_folders)

        setupViews()
        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupSearchView()

        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.setCheckedItem(R.id.nav_folders)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_all_notes -> {
                val intent = MainActivity.createIntent(this, 0)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            R.id.nav_favorites -> {
                val intent = MainActivity.createIntent(this, 0)
                intent.putExtra("view_type", MainActivity.ViewType.FAVORITES.name)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            R.id.nav_folders -> {
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setupViews() {
        recyclerViewFolders = findViewById(R.id.recyclerViewFolders)
        fabAddFolder = findViewById(R.id.fabAddFolder)
        searchView = findViewById(R.id.searchViewFolders)
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupRecyclerView() {
        folderAdapter = FolderAdapter(
            folders = DataHandler.getAllFolders(),
            onFolderClick = { folder ->
                val intent = MainActivity.createIntent(this, folder.id)
                startActivity(intent)
                finish() // Close the folders activity when a folder is selected
            },
            onFolderOptions = { folder ->
                FolderOptionsDialog(this).show(folder) {
                    folderAdapter.updateFolders(DataHandler.getAllFolders())
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
                    val filteredFolders = DataHandler.searchFolders(newText)
                    folderAdapter.updateFolders(filteredFolders)
                }
                return true
            }
        })
    }

    private fun showCreateFolderDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_folder, null)
        val editTextFolderName = view.findViewById<EditText>(R.id.editTextFolderName)
        val editTextFolderDescription = view.findViewById<EditText>(R.id.editTextFolderDescription)

        AlertDialog.Builder(this)
            .setTitle(R.string.create_folder)
            .setView(view)
            .setPositiveButton(R.string.create) { _, _ ->
                val folderName = editTextFolderName.text.toString().trim()
                val folderDescription = editTextFolderDescription.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    DataHandler.createFolder(folderName, folderDescription)
                    folderAdapter.updateFolders(DataHandler.getAllFolders())
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}