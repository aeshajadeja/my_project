package com.example.my_project

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class MarkAttendanceActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AttendanceAdapter
    private var studentList = mutableListOf<Student>()
    private val attendanceData = mutableMapOf<String, String>() // ID -> P/A
    private lateinit var subjectSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark_attendance)

        subjectSpinner = findViewById(R.id.subjectSpinner)
        val dateTv = findViewById<TextView>(R.id.dateTv)
        val saveBtn = findViewById<MaterialButton>(R.id.saveAttendanceBtn)
        val downloadBtn = findViewById<MaterialButton>(R.id.downloadReportBtn)
        val backBtn = findViewById<ImageButton>(R.id.backBtn)

        recyclerView = findViewById(R.id.attendanceRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupSubjectSpinner()

        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        dateTv.text = "Date: ${sdf.format(Date())}"

        loadStudents()

        backBtn.setOnClickListener { finish() }

        saveBtn.setOnClickListener {
            if (subjectSpinner.selectedItem == null) {
                Toast.makeText(this, "No subjects allocated to you", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveAttendance(subjectSpinner.selectedItem.toString())
        }

        downloadBtn.setOnClickListener {
            if (subjectSpinner.selectedItem == null) {
                Toast.makeText(this, "No subjects allocated to you", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            exportToPublicDownloads(subjectSpinner.selectedItem.toString())
        }
    }

    private fun setupSubjectSpinner() {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentFacultyId = userPref.getString("current_userId", "") ?: ""
        
        val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val allocatedSubjects = allocPref.getStringSet("subjects_$currentFacultyId", setOf())?.toList() ?: listOf()

        if (allocatedSubjects.isEmpty()) {
            val emptyList = listOf("No Subjects Allocated")
            val subAdapter = ArrayAdapter(this, R.layout.spinner_item, emptyList)
            subAdapter.setDropDownViewResource(R.layout.spinner_item)
            subjectSpinner.adapter = subAdapter
            subjectSpinner.isEnabled = false
        } else {
            val subAdapter = ArrayAdapter(this, R.layout.spinner_item, allocatedSubjects)
            subAdapter.setDropDownViewResource(R.layout.spinner_item)
            subjectSpinner.adapter = subAdapter
        }
    }

    private fun loadStudents() {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val allIds = sharedPref.getStringSet("all_user_ids", setOf()) ?: setOf()
        
        studentList.clear()
        for (id in allIds) {
            val role = sharedPref.getString("role_$id", "")
            if (role == "Student") {
                val name = sharedPref.getString("name_$id", "Student")!!
                studentList.add(Student(id, name))
            }
        }
        
        adapter = AttendanceAdapter(studentList) { id, status ->
            attendanceData[id] = status
        }
        recyclerView.adapter = adapter
    }

    private fun saveAttendance(subject: String) {
        if (subject == "No Subjects Allocated") return

        if (attendanceData.size < studentList.size) {
            Toast.makeText(this, "Please mark attendance for all students", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("AttendanceData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        
        for ((id, status) in attendanceData) {
            editor.putString("${subject}_${dateKey}_$id", status)
        }
        editor.apply()
        
        Toast.makeText(this, "Attendance saved for $subject", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun exportToPublicDownloads(subject: String) {
        if (subject == "No Subjects Allocated") return

        if (attendanceData.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "Attendance_${subject}_${System.currentTimeMillis()}.csv"
        val csvContent = StringBuilder()
        csvContent.append("Student ID,Student Name,Status\n")
        
        for (student in studentList) {
            val status = attendanceData[student.id] ?: "N/A"
            csvContent.append("${student.id},${student.name},$status\n")
        }

        try {
            val contentResolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri: Uri? = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    stream.write(csvContent.toString().toByteArray())
                }
                Toast.makeText(this, "File saved to Downloads folder!", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    data class Student(val id: String, val name: String)

    class AttendanceAdapter(private val list: List<Student>, val onMark: (String, String) -> Unit) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTv: TextView = view.findViewById(R.id.studentNameTv)
            val idTv: TextView = view.findViewById(R.id.studentIdTv)
            val rg: RadioGroup = view.findViewById(R.id.attendanceRadioGroup)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_student, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val student = list[position]
            h.nameTv.text = student.name
            h.idTv.text = "ID: ${student.id}"
            
            h.rg.setOnCheckedChangeListener { _, checkedId ->
                val status = if (checkedId == R.id.presentRb) "P" else "A"
                onMark(student.id, status)
            }
        }
        override fun getItemCount() = list.size
    }
}
