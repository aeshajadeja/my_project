package com.example.my_project

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var semesterSpinner: Spinner
    private val allSemesters = arrayOf("Select Sem", "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Header Views
        val welcomeTv = findViewById<TextView>(R.id.welcomeText)
        val profileIconView = findViewById<ImageView>(R.id.profileIconView)
        val profileCard = findViewById<MaterialCardView>(R.id.profileCard)
        val headerLogoutBtn = findViewById<ImageButton>(R.id.headerLogoutBtn)
        
        // Gatekeeping Views
        val yearSpinner = findViewById<Spinner>(R.id.homeYearSpinner)
        semesterSpinner = findViewById<Spinner>(R.id.homeSemesterSpinner)
        val lockNoteTv = findViewById<TextView>(R.id.lockNoteTv)
        val contentLayout = findViewById<LinearLayout>(R.id.homeContentLayout)

        // All Features (Inside contentLayout)
        val quizCard = findViewById<MaterialCardView>(R.id.quizCard)
        val attendanceCard = findViewById<MaterialCardView>(R.id.attendanceCard)
        val timetableCard = findViewById<MaterialCardView>(R.id.timetableCard)
        val resultsCard = findViewById<MaterialCardView>(R.id.resultsCard)
        val notifyCard = findViewById<MaterialCardView>(R.id.notifyCard)
        val assignmentsCard = findViewById<MaterialCardView>(R.id.assignmentsCard)
        val chatCard = findViewById<MaterialCardView>(R.id.chatCard)
        val libraryCard = findViewById<MaterialCardView>(R.id.libraryCard)
        val feesCard = findViewById<MaterialCardView>(R.id.feesCard)
        val eventsCard = findViewById<MaterialCardView>(R.id.eventsCard)
        val syllabusCard = findViewById<MaterialCardView>(R.id.studentSyllabusCard)

        // 1. Setup User Data
        refreshUserInfo(welcomeTv, profileIconView)

        // 2. Setup Selection Spinners
        val years = arrayOf("Select Batch", "2022-2025", "2023-2026", "2024-2027", "2025-2028")
        val yearAdapter = ArrayAdapter(this, R.layout.spinner_item, years)
        yearAdapter.setDropDownViewResource(R.layout.spinner_item)
        yearSpinner.adapter = yearAdapter

        // Initial Semester Spinner (Empty/Disabled)
        updateSemesterSpinner(emptyList())

        // Load saved selection or Auto-detect Batch from Enrollment No.
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = sharedPref.getString("current_userId", "") ?: ""
        
        // Logic: Auto-detect batch based on first two digits of Enrollment ID
        val autoDetectedBatchPos = when {
            currentId.startsWith("22") -> 1 // 2022-2025
            currentId.startsWith("23") -> 2 // 2023-2026
            currentId.startsWith("24") -> 3 // 2024-2027
            currentId.startsWith("25") -> 4 // 2025-2028
            else -> 0
        }

        if (autoDetectedBatchPos > 0) {
            yearSpinner.setSelection(autoDetectedBatchPos)
            yearSpinner.isEnabled = false // Lock batch selection for students
        } else {
            val savedYearPos = sharedPref.getInt("batch_pos_$currentId", 0)
            yearSpinner.setSelection(savedYearPos)
        }

        // 3. Selection Logic (Gatekeeping)
        val checkSelection = {
            if (yearSpinner.selectedItemPosition > 0 && semesterSpinner.selectedItemPosition > 0) {
                contentLayout.visibility = View.VISIBLE
                lockNoteTv.visibility = View.GONE
                
                // Save selection
                sharedPref.edit().apply {
                    putInt("batch_pos_$currentId", yearSpinner.selectedItemPosition)
                    putInt("sem_pos_$currentId", semesterSpinner.selectedItemPosition)
                    putString("current_sem_$currentId", semesterSpinner.selectedItem.toString())
                    apply()
                }
            } else {
                contentLayout.visibility = View.GONE
                lockNoteTv.visibility = View.VISIBLE
            }
        }

        yearSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position > 0) {
                    val batch = years[position]
                    val allowedSems = getAllowedSemestersForBatch(batch)
                    updateSemesterSpinner(allowedSems)
                    
                    // Restore saved sem if it's in the allowed list
                    val savedSem = sharedPref.getString("current_sem_$currentId", "")
                    val semIndex = allowedSems.indexOf(savedSem)
                    if (semIndex != -1) {
                        semesterSpinner.setSelection(semIndex)
                    }
                } else {
                    updateSemesterSpinner(emptyList())
                }
                checkSelection()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        semesterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { checkSelection() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // 4. Click Listeners for ALL features
        profileCard.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        quizCard.setOnClickListener { startActivity(Intent(this, QuizActivity::class.java)) }
        attendanceCard.setOnClickListener { startActivity(Intent(this, AttendanceActivity::class.java)) }
        timetableCard.setOnClickListener { startActivity(Intent(this, TimetableActivity::class.java)) }
        resultsCard.setOnClickListener { startActivity(Intent(this, ResultActivity::class.java)) }
        notifyCard.setOnClickListener { startActivity(Intent(this, NotificationsActivity::class.java)) }
        assignmentsCard.setOnClickListener { startActivity(Intent(this, AssignmentActivity::class.java)) }
        chatCard.setOnClickListener { startActivity(Intent(this, ChatUserListActivity::class.java)) }
        syllabusCard.setOnClickListener { startActivity(Intent(this, SyllabusActivity::class.java)) }
        
        libraryCard.setOnClickListener { Toast.makeText(this, "Opening E-Library...", Toast.LENGTH_SHORT).show() }
        feesCard.setOnClickListener { Toast.makeText(this, "Fee portal coming soon!", Toast.LENGTH_SHORT).show() }
        eventsCard.setOnClickListener { Toast.makeText(this, "Fetching events...", Toast.LENGTH_SHORT).show() }

        // HEADER LOGOUT
        headerLogoutBtn.setOnClickListener {
            val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
            sharedPref.edit().remove("current_userId").apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun getAllowedSemestersForBatch(batch: String): List<String> {
        val list = mutableListOf("Select Sem")
        when (batch) {
            "2023-2026" -> list.addAll(listOf("Sem 5", "Sem 6"))
            "2024-2027" -> list.addAll(listOf("Sem 3", "Sem 4"))
            "2025-2028" -> list.addAll(listOf("Sem 1", "Sem 2"))
            "2022-2025" -> list.addAll(listOf("Sem 7", "Sem 8")) // Final year
        }
        return list
    }

    private fun updateSemesterSpinner(allowedSems: List<String>) {
        if (allowedSems.isEmpty() || allowedSems.size == 1) {
            val placeholder = if (allowedSems.isEmpty()) listOf("Select Batch First") else allowedSems
            val adapter = ArrayAdapter(this, R.layout.spinner_item, placeholder)
            adapter.setDropDownViewResource(R.layout.spinner_item)
            semesterSpinner.adapter = adapter
            semesterSpinner.isEnabled = allowedSems.size > 1
        } else {
            val adapter = ArrayAdapter(this, R.layout.spinner_item, allowedSems)
            adapter.setDropDownViewResource(R.layout.spinner_item)
            semesterSpinner.adapter = adapter
            semesterSpinner.isEnabled = true
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUserInfo(findViewById(R.id.welcomeText), findViewById(R.id.profileIconView))
    }

    private fun refreshUserInfo(welcomeTv: TextView, iconView: ImageView) {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = sharedPref.getString("current_userId", "") ?: ""
        val fullName = sharedPref.getString("name_$currentId", "Student")
        val firstName = fullName?.split(" ")?.get(0) ?: "Student"
        val profileImgUri = sharedPref.getString("image_$currentId", null)

        welcomeTv.text = "Hi, $firstName!"
        if (profileImgUri != null) {
            try {
                iconView.setImageURI(Uri.parse(profileImgUri))
                iconView.setPadding(0, 0, 0, 0)
            } catch (e: Exception) {
                iconView.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
        }
    }
}
