package com.example.noteon

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonLogin: MaterialButton
    private lateinit var buttonSignUp: MaterialButton
    private lateinit var authStateManager: AuthStateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authStateManager = AuthStateManager.getInstance(this)

        if (GuestSession.getInstance(this).isGuestSession()) {
            if (hasGuestNotes()) {
                handleGuestDataOnLogin()
                return
            } else {
                GuestSession.getInstance(this).endGuestSession()
            }
        }

        setupViews()
        setupClickListeners()
    }

    private fun setupViews() {
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLogin = findViewById(R.id.buttonLogin)
        buttonSignUp = findViewById(R.id.buttonSignUp)
    }

    private fun setupClickListeners() {
        buttonLogin.setOnClickListener {
            userLogin()
        }

        buttonSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        findViewById<MaterialButton>(R.id.buttonContinueAsGuest).setOnClickListener {
            continueAsGuest()
        }
    }

    private fun userLogin() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (validateForm(email, password)) {
            showProgressBar()
            lifecycleScope.launch {
                try {
                    authStateManager.signIn(email, password) { success, error ->
                        hideProgressBar()
                        if (success) {
                            handleSuccessfulLogin()
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                error ?: getString(R.string.authentication_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    hideProgressBar()
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("LoginActivity", "Error during login", e)
                }
            }
        }
    }

    private fun handleSuccessfulLogin() {
        lifecycleScope.launch {
            try {
                when (val state = authStateManager.getCurrentState()) {
                    is AuthState.Authenticated -> {
                        // Restore data
                        AuthManager.getInstance(this@LoginActivity).restoreData()

                        // Check for guest data
                        val guestSession = GuestSession.getInstance(this@LoginActivity)
                        if (guestSession.isGuestSession() && hasGuestNotes()) {
                            handleGuestDataOnLogin()
                        } else {
                            guestSession.endGuestSession()
                            startMainActivity()
                        }
                    }
                    else -> startMainActivity()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during data sync", e)
                Toast.makeText(
                    this@LoginActivity,
                    "Error syncing data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun hasGuestNotes(): Boolean {
        val guestId = GuestSession.getInstance(this).getGuestId()
        return guestId?.let { id ->
            DataHandler.getAllNotes().any { it.userId == id }
        } ?: false
    }

    private fun validateForm(email: String, password: String): Boolean {
        return when {
            TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                editTextEmail.error = getString(R.string.invalid_email)
                false
            }
            TextUtils.isEmpty(password) -> {
                editTextPassword.error = getString(R.string.password_required)
                editTextEmail.error = null
                false
            }
            else -> {
                editTextEmail.error = null
                editTextPassword.error = null
                true
            }
        }
    }

    private fun continueAsGuest() {
        authStateManager.startGuestSession()
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun handleGuestDataOnLogin() {
        val guestSession = GuestSession.getInstance(this)
        val guestId = guestSession.getGuestId()
        val currentState = authStateManager.getCurrentState()

        if (guestId == null || currentState !is AuthState.Authenticated) {
            startMainActivity()
            return
        }

        DialogUtils.showGuestDataFoundDialog(
            context = this,
            onKeep = {
                DataHandler.convertGuestNotesToUser(guestId, currentState.user.uid)
                guestSession.endGuestSession()
                startMainActivity()
            },
            onDiscard = {
                DataHandler.clearGuestData(guestId)
                guestSession.endGuestSession()
                startMainActivity()
            }
        )
    }
}