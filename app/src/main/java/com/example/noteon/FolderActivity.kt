package com.example.noteon

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class FolderActivity : AppCompatActivity() {
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
    }

    private fun setupViews() {
        recyclerViewFolders = findViewById(R.id.recyclerViewFolders)
        fabAddFolder = findViewById(R.id.fabAddFolder)
        searchView = findViewById(R.id.searchViewFolders)
        drawerLayout = findViewById(R.id.drawerLayout)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupRecyclerView() {
        folderAdapter = FolderAdapter(DataHandler.getAllFolders()) { folder ->
            // Navigate to MainActivity with folder filter
            startActivity(MainActivity.createIntent(this, folder.id))
        }
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