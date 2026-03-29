package com.example.my_project

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Using findViewByID properly to avoid null pointer crashes
        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        backBtn?.setOnClickListener {
            finish()
        }
    }
}
