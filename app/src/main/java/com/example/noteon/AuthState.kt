package com.example.noteon

import com.google.firebase.auth.FirebaseUser

sealed class AuthState {
    data class Authenticated(val user: FirebaseUser) : AuthState()
    object Guest : AuthState()
    object Unauthenticated : AuthState()
}