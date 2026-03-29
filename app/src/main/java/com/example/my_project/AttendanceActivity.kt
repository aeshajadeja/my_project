package com.example.my_project

import android.content.Context
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AttendanceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        backBtn.setOnClickListener { finish() }

        // Update UI with REAL data from Faculty
        calculateAndDisplayAttendance()
    }

    private fun calculateAndDisplayAttendance() {
        val sharedPref = getSharedPreferences("AttendanceData", Context.MODE_PRIVATE)
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = userPref.getString("current_userId", "") ?: ""

        val subjects = listOf("Java", "Python", "C++", "Maths", "DBMS")
        val viewIds = mapOf(
            "Java" to R.id.javaAttendanceTv,
            "Python" to R.id.pythonAttendanceTv,
            "C++" to R.id.cppAttendanceTv,
            "Maths" to R.id.mathsAttendanceTv,
            "DBMS" to R.id.dbmsAttendanceTv
        )

        val allAttendance = sharedPref.all

        for (subject in subjects) {
            var totalLectures = 0
            var attendedLectures = 0

            for ((key, value) in allAttendance) {
                // Key format: ${subject}_${dateKey}_$id
                if (key.startsWith("${subject}_") && key.endsWith("_$currentId")) {
                    totalLectures++
                    if (value == "P") {
                        attendedLectures++
                    }
                }
            }

            val percentage = if (totalLectures > 0) {
                (attendedLectures * 100) / totalLectures
            } else {
                0
            }

            val tv = findViewById<TextView>(viewIds[subject]!!)
            tv.text = "$percentage%"
        }
    }
}
