package com.example.my_project

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class CourseManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_management)

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener {
            finish()
        }
    }
}
