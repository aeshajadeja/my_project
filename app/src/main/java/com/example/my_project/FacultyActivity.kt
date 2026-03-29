package com.example.my_project

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class FacultyActivity : AppCompatActivity() {

    private lateinit var profileIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty)

        profileIcon = findViewById(R.id.facultyProfileIconView)
        val attendanceCard = findViewById<MaterialCardView>(R.id.facultyAttendanceCard)
        val scheduleCard = findViewById<MaterialCardView>(R.id.facultyScheduleCard)
        val assignmentsCard = findViewById<MaterialCardView>(R.id.facultyAssignmentsCard)
        val examResultCard = findViewById<MaterialCardView>(R.id.facultyExamResultCard)
        val syllabusCard = findViewById<MaterialCardView>(R.id.facultySyllabusCard)
        val noticeCard = findViewById<MaterialCardView>(R.id.facultyNoticeCard)
        val profileCard = findViewById<MaterialCardView>(R.id.facultyProfileCard)
        val logoutBtn = findViewById<ImageView>(R.id.logoutBtn)

        refreshFacultyInfo()

        profileCard.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        attendanceCard.setOnClickListener {
            startActivity(Intent(this, MarkAttendanceActivity::class.java))
        }

        scheduleCard.setOnClickListener {
            startActivity(Intent(this, FacultyTimetableActivity::class.java))
        }

        assignmentsCard.setOnClickListener {
            startActivity(Intent(this, FacultyAssignmentActivity::class.java))
        }

        examResultCard.setOnClickListener {
            startActivity(Intent(this, FacultyExamResultActivity::class.java))
        }

        syllabusCard.setOnClickListener {
            startActivity(Intent(this, SyllabusActivity::class.java))
        }

        noticeCard.setOnClickListener {
            startActivity(Intent(this, NoticeManagementActivity::class.java))
        }

        logoutBtn.setOnClickListener {
            val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
            sharedPref.edit().remove("current_userId").apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshFacultyInfo()
    }

    private fun refreshFacultyInfo() {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = sharedPref.getString("current_userId", "") ?: ""
        val name = sharedPref.getString("name_$currentId", "Professor")
        val imgUri = sharedPref.getString("image_$currentId", null)

        findViewById<TextView>(R.id.facultyWelcomeTv).text = "Welcome, $name"
        if (imgUri != null) {
            try {
                profileIcon.setImageURI(Uri.parse(imgUri))
                profileIcon.setPadding(0, 0, 0, 0)
            } catch (e: Exception) {}
        }
    }
}
