package com.example.my_project

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView

class AdminActivity : AppCompatActivity() {

    private lateinit var totalStudentsTv: TextView
    private lateinit var totalFacultyTv: TextView
    private lateinit var profileIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        totalStudentsTv = findViewById(R.id.statTotalStudents)
        totalFacultyTv = findViewById(R.id.statTotalFaculty)
        profileIcon = findViewById(R.id.adminProfileIconView)

        val facultyCard = findViewById<MaterialCardView>(R.id.facultyManagementCard)
        val studentCard = findViewById<MaterialCardView>(R.id.studentManagementCard)
        val noticeCard = findViewById<MaterialCardView>(R.id.noticeManagementCard)
        val courseCard = findViewById<MaterialCardView>(R.id.courseManagementCard)
        val allocationCard = findViewById<MaterialCardView>(R.id.allocationCard)
        val scheduleCard = findViewById<MaterialCardView>(R.id.scheduleManagementCard)
        val profileCard = findViewById<MaterialCardView>(R.id.adminProfileCard)
        val logoutBtn = findViewById<ImageView>(R.id.logoutBtn)

        refreshAdminInfo()
        updateStats()

        profileCard.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        facultyCard.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("ROLE_FILTER", "Faculty")
            startActivity(intent)
        }

        studentCard.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("ROLE_FILTER", "Student")
            startActivity(intent)
        }

        noticeCard.setOnClickListener {
            startActivity(Intent(this, NoticeManagementActivity::class.java))
        }

        courseCard.setOnClickListener {
            startActivity(Intent(this, CourseManagementActivity::class.java))
        }

        allocationCard.setOnClickListener {
            startActivity(Intent(this, SubjectAllocationActivity::class.java))
        }

        scheduleCard.setOnClickListener {
            startActivity(Intent(this, AdminScheduleActivity::class.java))
        }

        logoutBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to exit Admin Dashboard?")
                .setPositiveButton("Yes") { _, _ ->
                    val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    sharedPref.edit().remove("current_userId").apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAdminInfo()
        updateStats()
    }

    private fun refreshAdminInfo() {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = sharedPref.getString("current_userId", "") ?: ""
        val name = sharedPref.getString("name_$currentId", "Administrator")
        val imgUri = sharedPref.getString("image_$currentId", null)

        findViewById<TextView>(R.id.adminWelcomeTv).text = "Welcome, $name"
        if (imgUri != null) {
            try {
                profileIcon.setImageURI(Uri.parse(imgUri))
                profileIcon.setPadding(0, 0, 0, 0)
            } catch (e: Exception) {}
        }
    }

    private fun updateStats() {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val allIds = sharedPref.getStringSet("all_user_ids", setOf()) ?: setOf()
        var studentCount = 0
        var facultyCount = 0
        for (id in allIds) {
            val role = sharedPref.getString("role_$id", "")
            if (role == "Student") studentCount++
            if (role == "Faculty") facultyCount++
        }
        totalStudentsTv.text = studentCount.toString()
        totalFacultyTv.text = facultyCount.toString()
    }
}
