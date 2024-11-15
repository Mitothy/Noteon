package com.example.noteon

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var switchIntelligentSearch: SwitchMaterial
    private lateinit var switchSmartCategorization: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupToolbar()
        setupSwitches()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
    }

    private fun setupSwitches() {
        switchIntelligentSearch = findViewById(R.id.switchIntelligentSearch)
        switchSmartCategorization = findViewById(R.id.switchSmartCategorization)

        // Load saved preferences (to be implemented)
        switchIntelligentSearch.isChecked = false
        switchSmartCategorization.isChecked = false

        // Setup listeners (to be implemented)
        switchIntelligentSearch.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Implement intelligent search toggle functionality
        }

        switchSmartCategorization.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Implement smart categorization toggle functionality
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}