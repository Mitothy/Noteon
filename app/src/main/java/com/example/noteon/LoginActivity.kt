package com.example.noteon

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonLogin: MaterialButton
    private lateinit var buttonSignUp: MaterialButton
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupViews()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            updateUI(currentUser)
        }
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


        /* findViewById<MaterialButton>(R.id.buttonSignInWithGoogle)?.setOnClickListener {
            signInWithGoogle()
        }
         */

        findViewById<MaterialButton>(R.id.buttonContinueAsGuest).setOnClickListener {
            // Start guest session
            GuestSession.getInstance(this).startGuestSession()

            // Start MainActivity as guest
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun userLogin() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (validateForm(email, password)) {
            showProgressBar()
            lifecycleScope.launch {
                try {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this@LoginActivity) { task ->
                            if (task.isSuccessful) {
                                // Restore notes in a coroutine
                                lifecycleScope.launch {
                                    try {
                                        val authManager = AuthManager.getInstance(this@LoginActivity)

                                        // First restore folders to ensure proper hierarchy
                                        authManager.restoreFolders()

                                        // Then restore notes
                                        authManager.restoreNotes()

                                        val guestSession = GuestSession.getInstance(this@LoginActivity)
                                        if (guestSession.isGuestSession() && hasGuestNotes()) {
                                            handleGuestDataOnLogin()
                                        } else {
                                            guestSession.endGuestSession()
                                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                            finish()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("LoginActivity", "Error during sync: ${e.message}", e)
                                        hideProgressBar()
                                        Toast.makeText(
                                            this@LoginActivity,
                                            "Error syncing data: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                hideProgressBar()
                                Toast.makeText(
                                    this@LoginActivity,
                                    "Authentication failed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } catch (e: Exception) {
                    hideProgressBar()
                    Toast.makeText(
                        this@LoginActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
                editTextEmail.error = "Enter valid email address"
                false
            }
            TextUtils.isEmpty(password) -> {
                editTextPassword.error = "Enter password"
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

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun showProgressBar() {
        // Implement progress bar show logic
    }

    private fun hideProgressBar() {
        // Implement progress bar hide logic
    }

    private fun handleGuestDataOnLogin() {
        val guestSession = GuestSession.getInstance(this)
        val guestId = guestSession.getGuestId()
        val userId = auth.currentUser?.uid

        if (guestId == null || userId == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        DialogUtils.showGuestDataFoundDialog(
            context = this,
            onKeep = {
                DataHandler.convertGuestNotesToUser(guestId, userId)
                guestSession.endGuestSession()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
            onDiscard = {
                DataHandler.clearGuestData(guestId)
                guestSession.endGuestSession()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        )
    }
}