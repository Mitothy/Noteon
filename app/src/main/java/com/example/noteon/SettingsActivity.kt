package com.example.noteon

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private lateinit var switchIntelligentSearch: SwitchMaterial
    private lateinit var switchSmartCategorization: SwitchMaterial
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var authManager: AuthManager
    private lateinit var guestSession: GuestSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager.getInstance(this)
        authManager = AuthManager.getInstance(this)
        guestSession = GuestSession.getInstance(this)

        setContentView(R.layout.activity_settings)

        setupToolbar()
        setupSwitches()
        loadSettings()
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            try {
                // If authenticated user, try to restore settings from Firebase
                if (!guestSession.isGuestSession() && authManager.currentUser != null) {
                    authManager.currentUser?.let { user ->
                        preferencesManager.restoreSettingsFromFirebase(user.uid)
                    }
                }
            } catch (e: Exception) {
                // If restore fails, use local settings
            } finally {
                // Update switch states with current settings
                switchIntelligentSearch.isChecked = preferencesManager.isIntelligentSearchEnabled()
                switchSmartCategorization.isChecked = preferencesManager.isSmartCategorizationEnabled()

                // Enable/disable switches based on guest mode
                val enabled = !guestSession.isGuestSession()
                switchIntelligentSearch.isEnabled = enabled
                switchSmartCategorization.isEnabled = enabled
            }
        }
    }

    private fun setupSwitches() {
        switchIntelligentSearch = findViewById(R.id.switchIntelligentSearch)
        switchSmartCategorization = findViewById(R.id.switchSmartCategorization)

        // Setup listeners
        switchIntelligentSearch.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setIntelligentSearchEnabled(isChecked)
            syncSettings()
        }

        switchSmartCategorization.setOnCheckedChangeListener { _, isChecked ->
            preferencesManager.setSmartCategorizationEnabled(isChecked)
            syncSettings()
        }

        // Initially disable switches if in guest mode
        if (guestSession.isGuestSession()) {
            switchIntelligentSearch.isEnabled = false
            switchSmartCategorization.isEnabled = false
        }
    }

    private fun syncSettings() {
        if (!guestSession.isGuestSession()) {
            authManager.currentUser?.let { user ->
                preferencesManager.syncSettingsToFirebase(user.uid)
            }
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