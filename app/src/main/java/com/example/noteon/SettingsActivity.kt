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
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager.getInstance(this)
        setContentView(R.layout.activity_settings)

        setupToolbar()
        setupSwitches()
    }

    override fun onResume() {
        super.onResume()
        switchIntelligentSearch.isChecked = preferencesManager.isIntelligentSearchEnabled()
        switchSmartCategorization.isChecked = preferencesManager.isSmartCategorizationEnabled()
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

        // Load saved preferences
        switchIntelligentSearch.isChecked = preferencesManager.isSmartCategorizationEnabled()
        switchSmartCategorization.isChecked = preferencesManager.isSmartCategorizationEnabled()

        // Setup listeners
        switchIntelligentSearch.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setIntelligentSearchEnabled(isChecked)
        }

        switchSmartCategorization.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setSmartCategorizationEnabled(isChecked)
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