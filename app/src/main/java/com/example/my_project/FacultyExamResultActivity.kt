package com.example.my_project

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class FacultyExamResultActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MarksAdapter
    private var studentList = mutableListOf<StudentMark>()
    private val marksData = mutableMapOf<String, String>() // ID -> Mark
    private lateinit var subjectSpinner: Spinner
    private var facultyAllocations = mutableListOf<Pair<String, String>>() // Subject to Semester

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty_exam_result)

        subjectSpinner = findViewById(R.id.subjectSpinner)
        recyclerView = findViewById(R.id.marksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupSubjectSpinner()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        
        findViewById<MaterialButton>(R.id.createTestBtn).setOnClickListener {
            val subject = if (subjectSpinner.isEnabled && subjectSpinner.selectedItem != null) {
                facultyAllocations[subjectSpinner.selectedItemPosition].first
            } else "N/A"
            
            if (subject == "N/A" || subject == "No Subjects Allocated") {
                Toast.makeText(this, "No subjects allocated to you", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Internal Test Created for $subject", Toast.LENGTH_SHORT).show()
        }

        findViewById<MaterialButton>(R.id.saveMarksBtn).setOnClickListener {
            val subject = if (subjectSpinner.isEnabled && subjectSpinner.selectedItem != null) {
                facultyAllocations[subjectSpinner.selectedItemPosition].first
            } else "N/A"
            
            if (subject == "N/A" || subject == "No Subjects Allocated") {
                Toast.makeText(this, "No subjects allocated to you", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveMarks(subject)
        }

        subjectSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (facultyAllocations.isNotEmpty()) {
                    val subject = facultyAllocations[position].first
                    val sem = facultyAllocations[position].second
                    loadStudents(sem)
                    loadSavedMarksForSubject(subject)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSubjectSpinner() {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentFacultyId = userPref.getString("current_userId", "") ?: ""
        
        val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val allAllocIds = allocPref.getStringSet("allocation_ids", setOf()) ?: setOf()
        
        facultyAllocations.clear()
        for (allocId in allAllocIds) {
            val fId = allocPref.getString("fac_id_$allocId", "")
            if (fId == currentId(currentFacultyId)) { // Helper just in case
                val sub = allocPref.getString("subject_$allocId", "") ?: ""
                val sem = allocPref.getString("semester_$allocId", "") ?: ""
                facultyAllocations.add(sub to sem)
            }
        }

        if (facultyAllocations.isEmpty()) {
            val emptyList = listOf("No Subjects Allocated")
            val subAdapter = ArrayAdapter(this, R.layout.spinner_item, emptyList)
            subAdapter.setDropDownViewResource(R.layout.spinner_item)
            subjectSpinner.adapter = subAdapter
            subjectSpinner.isEnabled = false
        } else {
            val displayList = facultyAllocations.map { "${it.first} (${it.second})" }
            val subAdapter = ArrayAdapter(this, R.layout.spinner_item, displayList)
            subAdapter.setDropDownViewResource(R.layout.spinner_item)
            subjectSpinner.adapter = subAdapter
        }
    }
    
    private fun currentId(id: String) = id

    private fun loadStudents(targetSem: String) {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val allIds = sharedPref.getStringSet("all_user_ids", setOf()) ?: setOf()
        
        studentList.clear()
        marksData.clear()
        for (id in allIds) {
            val role = sharedPref.getString("role_$id", "")
            val studentSem = sharedPref.getString("current_sem_$id", "All")
            
            if (role == "Student" && (targetSem == "All" || studentSem == targetSem)) {
                val name = sharedPref.getString("name_$id", "Student")!!
                studentList.add(StudentMark(id, name))
            }
        }
        
        adapter = MarksAdapter(studentList) { id, mark ->
            marksData[id] = mark
        }
        recyclerView.adapter = adapter
    }

    private fun loadSavedMarksForSubject(subject: String) {
        // Implementation for loading existing marks if needed
    }

    private fun saveMarks(subject: String) {
        if (marksData.isEmpty()) {
            Toast.makeText(this, "Please enter marks for students", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("ExamData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        
        var total = 0
        var count = 0
        var maxMark = -1
        var topperName = ""

        for ((id, mark) in marksData) {
            editor.putString("mark_${subject}_$id", mark)
            val m = mark.toIntOrNull() ?: 0
            total += m
            count++
            if (m > maxMark) {
                maxMark = m
                topperName = studentList.find { it.id == id }?.name ?: "Unknown"
            }
        }
        
        editor.apply()
        
        // Update Stats
        val avg = if (count > 0) total / count else 0
        findViewById<TextView>(R.id.statsAverageTv).text = "Average Score ($subject): $avg%"
        findViewById<TextView>(R.id.statsTopperTv).text = "Topper: $topperName ($maxMark%)"

        Toast.makeText(this, "Marks saved for $subject and statistics updated!", Toast.LENGTH_SHORT).show()
    }

    data class StudentMark(val id: String, val name: String)

    class MarksAdapter(private val list: List<StudentMark>, val onMarkEntry: (String, String) -> Unit) : RecyclerView.Adapter<MarksAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val nameTv: TextView = v.findViewById(R.id.studentNameTv)
            val idTv: TextView = v.findViewById(R.id.studentIdTv)
            val markEt: EditText = v.findViewById(R.id.markEt)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_student_mark, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val s = list[position]
            h.nameTv.text = s.name
            h.idTv.text = "ID: ${s.id}"
            
            h.markEt.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    onMarkEntry(s.id, h.markEt.text.toString())
                }
            }
        }
        override fun getItemCount() = list.size
    }
}
