package com.example.my_project

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        backBtn?.setOnClickListener {
            finish()
        }

        displayDetailedResults()
    }

    private fun displayDetailedResults() {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = userPref.getString("current_userId", "") ?: ""
        
        val examPref = getSharedPreferences("ExamData", Context.MODE_PRIVATE)
        val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        
        // This is where we calculate GPA or display individual subject marks
        // For now, let's update the summary card if needed or add a list of subject marks
    }
}
