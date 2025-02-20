package com.example.noteon

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

open class BaseActivity : AppCompatActivity() {
    private lateinit var pb: Dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun showProgressBar() {
        pb = Dialog(this)
        pb.setContentView(R.layout.dialog_progress)
        pb.setCancelable(false)
        pb.show()
    }

    fun hideProgressBar() {
        if (::pb.isInitialized && pb.isShowing) {
            pb.dismiss()
        }
    }
}