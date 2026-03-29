package com.example.my_project

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.*

class SyllabusActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SyllabusAdapter
    private var syllabusList = mutableListOf<Syllabus>()
    
    private var selectedPdfUri: Uri? = null
    private var pdfStatusTv: TextView? = null

    private val pdfPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedPdfUri = result.data?.data
            pdfStatusTv?.text = "📄 PDF Selected"
            pdfStatusTv?.setTextColor(resources.getColor(R.color.success))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_syllabus)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val uploadBtn = findViewById<MaterialButton>(R.id.uploadSyllabusBtn)
        recyclerView = findViewById(R.id.syllabusRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = userPref.getString("current_userId", "") ?: ""
        val role = userPref.getString("role_$currentId", "")

        if (role == "Faculty") {
            uploadBtn.visibility = View.VISIBLE
        }

        backBtn.setOnClickListener { finish() }
        uploadBtn.setOnClickListener { showUploadDialog() }

        loadSyllabus()
    }

    private fun loadSyllabus() {
        val sharedPref = getSharedPreferences("SyllabusData", Context.MODE_PRIVATE)
        val ids = sharedPref.getStringSet("syllabus_ids", setOf()) ?: setOf()
        
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = userPref.getString("current_userId", "") ?: ""
        val role = userPref.getString("role_$currentId", "")
        val currentSem = userPref.getString("current_sem_$currentId", "All") ?: "All"

        syllabusList.clear()
        for (id in ids) {
            val sem = sharedPref.getString("sem_$id", "All") ?: "All"
            
            // Filter: Student sees only their sem; Faculty sees all
            if (role == "Faculty" || sem == "All" || sem == currentSem) {
                val subject = sharedPref.getString("subject_$id", "") ?: ""
                val faculty = sharedPref.getString("faculty_$id", "") ?: ""
                val pdfUri = sharedPref.getString("uri_$id", "") ?: ""
                syllabusList.add(Syllabus(id, subject, faculty, sem, pdfUri))
            }
        }
        
        adapter = SyllabusAdapter(syllabusList) { syllabus -> openPdf(syllabus) }
        recyclerView.adapter = adapter
        
        if (syllabusList.isEmpty()) {
            Toast.makeText(this, "No syllabus found for $currentSem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUploadDialog() {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = userPref.getString("current_userId", "") ?: ""
        val facultyName = userPref.getString("name_$currentId", "Faculty") ?: "Faculty"
        
        val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val allocatedSubjects = allocPref.getStringSet("subjects_$currentId", setOf())?.toList() ?: listOf()

        if (allocatedSubjects.isEmpty()) {
            Toast.makeText(this, "No subjects allocated to you", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Upload Syllabus PDF")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val subjectSpinner = Spinner(this).apply {
            val subAdapter = ArrayAdapter(context, R.layout.spinner_item, allocatedSubjects)
            subAdapter.setDropDownViewResource(R.layout.spinner_item)
            adapter = subAdapter
        }

        val sems = arrayOf("All", "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8")
        val semSpinner = Spinner(this).apply {
            val semAdapter = ArrayAdapter(context, R.layout.spinner_item, sems)
            semAdapter.setDropDownViewResource(R.layout.spinner_item)
            adapter = semAdapter
        }

        val selectPdfBtn = Button(this).apply { text = "Select PDF File" }
        pdfStatusTv = TextView(this).apply { 
            text = "No file selected"
            setPadding(0, 10, 0, 20)
        }

        selectPdfBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/pdf"
            }
            pdfPickerLauncher.launch(intent)
        }

        layout.addView(TextView(this).apply { text = "Select Subject:" })
        layout.addView(subjectSpinner)
        layout.addView(TextView(this).apply { text = "Select Semester:" })
        layout.addView(semSpinner)
        layout.addView(selectPdfBtn)
        layout.addView(pdfStatusTv)
        
        builder.setView(layout)
        builder.setPositiveButton("Upload") { _, _ ->
            if (selectedPdfUri != null) {
                saveSyllabus(
                    subjectSpinner.selectedItem.toString(),
                    facultyName,
                    semSpinner.selectedItem.toString(),
                    selectedPdfUri!!
                )
                loadSyllabus()
            } else {
                Toast.makeText(this, "Please select a PDF", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveSyllabus(subject: String, faculty: String, sem: String, uri: Uri) {
        val sharedPref = getSharedPreferences("SyllabusData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val id = UUID.randomUUID().toString()

        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {}

        editor.putString("subject_$id", subject)
        editor.putString("faculty_$id", faculty)
        editor.putString("sem_$id", sem)
        editor.putString("uri_$id", uri.toString())

        val ids = sharedPref.getStringSet("syllabus_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = ids.toMutableSet()
        updatedIds.add(id)
        editor.putStringSet("syllabus_ids", updatedIds)
        editor.apply()
        
        Toast.makeText(this, "Syllabus uploaded successfully", Toast.LENGTH_SHORT).show()
    }

    private fun openPdf(syllabus: Syllabus) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(syllabus.pdfUri), "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }
    }

    data class Syllabus(val id: String, val subject: String, val faculty: String, val sem: String, val pdfUri: String)

    class SyllabusAdapter(private val list: List<Syllabus>, val onView: (Syllabus) -> Unit) : RecyclerView.Adapter<SyllabusAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subjectTv: TextView = v.findViewById(R.id.syllabusSubjectTv)
            val facultyTv: TextView = v.findViewById(R.id.syllabusFacultyTv)
            val viewBtn: Button = v.findViewById(R.id.viewPdfBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_syllabus, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val s = list[position]
            h.subjectTv.text = s.subject
            h.facultyTv.text = "By: ${s.faculty} (${s.sem})"
            h.viewBtn.setOnClickListener { onView(s) }
        }
        override fun getItemCount() = list.size
    }
}
