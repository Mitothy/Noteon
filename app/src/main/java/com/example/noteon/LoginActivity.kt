package com.example.noteon

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
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
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Check if coming from guest mode
                        val guestSession = GuestSession.getInstance(this)
                        if (guestSession.isGuestSession()) {
                            handleGuestDataOnLogin()
                        } else {
                            // Direct login without guest data
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                    }
                    hideProgressBar()
                }
        }
    }


    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleGoogleSignInResult(task)
        }
    }

    private fun handleGoogleSignInResult(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUIWithGoogleAccount(account)
            }
        } else {
            Toast.makeText(this, "Sign In Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIWithGoogleAccount(account: GoogleSignInAccount) {
        showProgressBar()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val user = User(
                    id = auth.currentUser?.uid ?: "",
                    name = account.displayName ?: "",
                    email = account.email ?: ""
                )
                // Store user info in database
                DataHandler.storeUserInfo(user)

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Sign In Failed", Toast.LENGTH_SHORT).show()
            }
            hideProgressBar()
        }
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
        // Show dialog to user
        AlertDialog.Builder(this)
            .setTitle(R.string.guest_data_found)
            .setMessage(R.string.convert_guest_data_message)
            .setPositiveButton(R.string.convert) { _, _ ->
                // Convert guest data to user data
                guestSession.getGuestId()?.let { guestId ->
                    auth.currentUser?.uid?.let { userId ->
                        DataHandler.convertGuestNotesToUser(guestId, userId)
                    }
                }
                // End guest session after conversion
                guestSession.endGuestSession()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setNegativeButton(R.string.discard) { _, _ ->
                // Clear guest data
                guestSession.clearGuestData(this)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .setCancelable(false)
            .show()
    }
}