package com.example.noteon

import android.app.Activity
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

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

    fun showToast(activity: Activity, msg: String) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }
}