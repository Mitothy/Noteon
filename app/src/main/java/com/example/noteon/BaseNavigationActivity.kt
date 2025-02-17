package com.example.noteon

import android.content.Intent
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView

abstract class BaseNavigationActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected abstract val drawerLayout: DrawerLayout
    protected abstract val currentNavigationItem: Int

    // Optional method for activities to handle navigation changes
    protected open fun onNavigationChanged(itemId: Int) {}

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

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
                if (GuestSession.getInstance(this).isGuestSession()) {
                    DialogUtils.showExitGuestModeDialog(
                        context = this,
                        onConfirm = {
                            GuestSession.getInstance(this).clearGuestData(this)
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                    )
                } else if (AuthManager.getInstance(this).currentUser != null) {
                    AuthManager.getInstance(this).signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
        }

        onNavigationChanged(item.itemId)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}