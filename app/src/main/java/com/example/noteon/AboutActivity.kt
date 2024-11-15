package com.example.noteon

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView

class AboutActivity : AppCompatActivity() {
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.about)
    }

    private fun setupClickListeners() {
        findViewById<MaterialCardView>(R.id.cardPrivacyPolicy).setOnClickListener {
            openWebPage("https://youtu.be/dQw4w9WgXcQ?si=HhGysGtGGl2F8K8j")
        }

        findViewById<MaterialCardView>(R.id.cardTermsOfService).setOnClickListener {
            openWebPage("https://youtu.be/dQw4w9WgXcQ?si=HhGysGtGGl2F8K8j")
        }
    }

    private fun openWebPage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
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