package com.example.noteon

data class User(
    val id: String,
    val name: String,
    val email: String,
    val image: String = ""  // Optional profile image
)