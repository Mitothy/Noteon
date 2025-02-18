package com.example.noteon

import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import com.google.firebase.auth.FirebaseUser

class AuthStateManager private constructor(private val context: Context) {
    private var currentState: AuthState = AuthState.Unauthenticated
    private val stateObservers = mutableListOf<(AuthState) -> Unit>()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val guestSession: GuestSession = GuestSession.getInstance(context)

    companion object {
        @Volatile
        private var instance: AuthStateManager? = null

        fun getInstance(context: Context): AuthStateManager {
            return instance ?: synchronized(this) {
                instance ?: AuthStateManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        // Initialize state based on current auth status
        currentState = when {
            auth.currentUser != null -> AuthState.Authenticated(auth.currentUser!!)
            guestSession.isGuestSession() -> AuthState.Guest
            else -> AuthState.Unauthenticated
        }
    }

    fun getCurrentState(): AuthState = currentState

    fun observeState(observer: (AuthState) -> Unit) {
        stateObservers.add(observer)
        // Immediately notify of current state
        observer(currentState)
    }

    fun removeObserver(observer: (AuthState) -> Unit) {
        stateObservers.remove(observer)
    }

    fun signIn(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful && auth.currentUser != null) {
                    transitionToAuthenticated(auth.currentUser!!)
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun signOut() {
        when (currentState) {
            is AuthState.Authenticated -> {
                auth.signOut()
                transitionToUnauthenticated()
            }
            is AuthState.Guest -> {
                guestSession.clearGuestData(context)
                transitionToUnauthenticated()
            }
            else -> {
                // Already unauthenticated, do nothing
            }
        }
    }

    fun startGuestSession() {
        guestSession.startGuestSession()
        transitionToGuest()
    }

    private fun transitionToAuthenticated(user: FirebaseUser) {
        currentState = AuthState.Authenticated(user)
        notifyObservers()
    }

    private fun transitionToGuest() {
        currentState = AuthState.Guest
        notifyObservers()
    }

    private fun transitionToUnauthenticated() {
        currentState = AuthState.Unauthenticated
        notifyObservers()
    }

    private fun notifyObservers() {
        stateObservers.forEach { it(currentState) }
    }
}