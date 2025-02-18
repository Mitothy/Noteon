package com.example.noteon

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView

abstract class BaseNavigationActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected abstract val drawerLayout: DrawerLayout
    protected abstract val currentNavigationItem: Int
    lateinit var authStateManager: AuthStateManager
    private var stateObserver: ((AuthState) -> Unit)? = null

    // Optional method for activities to handle navigation changes
    protected open fun onNavigationChanged(itemId: Int) {}

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        authStateManager = AuthStateManager.getInstance(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // Setup state observer after layout is initialized
        setupStateObserver()
    }

    private fun setupStateObserver() {
        stateObserver = { state ->
            updateUIForAuthState(state)
        }
        stateObserver?.let { authStateManager.observeState(it) }
    }

    private fun updateUIForAuthState(state: AuthState) {
        val navigationView = findViewById<NavigationView>(R.id.navigationView) ?: return
        val headerView = navigationView.getHeaderView(0) ?: return
        val footerContainer = navigationView.findViewById<View>(R.id.nav_footer_container)
        val usernameTextView = headerView.findViewById<TextView>(R.id.nav_header_username)
        val emailTextView = headerView.findViewById<TextView>(R.id.nav_header_email)

        when (state) {
            is AuthState.Authenticated -> {
                footerContainer?.visibility = View.GONE
                val userName = DataHandler.getUserName(state.user.uid)
                usernameTextView?.text = userName ?: state.user.email?.substringBefore('@')?.capitalize()
                emailTextView?.text = state.user.email
            }
            is AuthState.Guest -> {
                footerContainer?.visibility = View.VISIBLE
                usernameTextView?.text = getString(R.string.app_name)
                emailTextView?.text = getString(R.string.guest_user)
            }
            is AuthState.Unauthenticated -> {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    protected fun setupNavigationFooter() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val loginButton = navigationView.findViewById<MaterialButton>(R.id.buttonLogin)
        val signUpButton = navigationView.findViewById<MaterialButton>(R.id.buttonSignUp)

        loginButton?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        signUpButton?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Don't do anything if we're already on the selected item
        if (item.itemId == currentNavigationItem) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }

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
                if (this !is FolderActivity) {
                    startActivity(Intent(this, FolderActivity::class.java))
                    finish()
                }
            }
            R.id.nav_trash -> {
                val intent = MainActivity.createIntent(this, 0)
                intent.putExtra("view_type", MainActivity.ViewType.TRASH.name)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_sign_out -> {
                handleSignOut()
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
        }

        onNavigationChanged(item.itemId)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    protected fun handleSignOut() {
        when (authStateManager.getCurrentState()) {
            is AuthState.Guest -> {
                showExitGuestModeDialog()
            }
            is AuthState.Authenticated -> {
                authStateManager.signOut()
            }
            else -> {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
    }

    private fun showExitGuestModeDialog() {
        DialogUtils.showExitGuestModeDialog(
            context = this,
            onConfirm = {
                authStateManager.signOut()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stateObserver?.let { authStateManager.removeObserver(it) }
    }
}