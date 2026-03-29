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

class FacultyAssignmentActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AssignmentAdapter
    private var assignmentList = mutableListOf<Assignment>()
    
    private var selectedDocUri: Uri? = null
    private var docStatusTv: TextView? = null

    // Launcher for selecting Assignment Document
    private val docPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedDocUri = result.data?.data
            docStatusTv?.text = "📄 Document Selected"
            docStatusTv?.setTextColor(resources.getColor(R.color.success))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty_assignment)

        recyclerView = findViewById(R.id.assignmentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadAssignments()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.addAssignmentBtn).setOnClickListener { showAddAssignmentDialog() }
    }

    private fun loadAssignments() {
        val sharedPref = getSharedPreferences("AssignmentData", Context.MODE_PRIVATE)
        val assignIds = sharedPref.getStringSet("assign_ids", mutableSetOf()) ?: mutableSetOf()
        
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentFacultyId = userPref.getString("current_userId", "") ?: ""

        assignmentList.clear()
        for (id in assignIds) {
            val facultyId = sharedPref.getString("facultyId_$id", "")
            // Only show assignments created by this faculty
            if (facultyId == currentFacultyId) {
                val title = sharedPref.getString("title_$id", "") ?: ""
                val deadline = sharedPref.getString("deadline_$id", "") ?: ""
                val docUri = sharedPref.getString("doc_$id", null)
                val subject = sharedPref.getString("subject_$id", "General") ?: "General"
                assignmentList.add(Assignment(id, title, deadline, docUri, subject))
            }
        }
        
        adapter = AssignmentAdapter(assignmentList, 
            { assign: Assignment -> showEditDialog(assign) }, 
            { assign: Assignment -> deleteAssignment(assign) },
            { assign: Assignment -> viewSubmissions(assign) }
        )
        recyclerView.adapter = adapter
    }

    private fun showAddAssignmentDialog() {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentFacultyId = userPref.getString("current_userId", "") ?: ""
        
        val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val allocatedSubjects = allocPref.getStringSet("subjects_$currentFacultyId", setOf())?.toList() ?: listOf()

        if (allocatedSubjects.isEmpty()) {
            Toast.makeText(this, "No subjects allocated to you. Contact Admin.", Toast.LENGTH_LONG).show()
            return
        }

        selectedDocUri = null // Reset for new upload
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Upload New Assignment")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val subjectSpinner = Spinner(this).apply {
            val subAdapter = ArrayAdapter(context, R.layout.spinner_item, allocatedSubjects)
            subAdapter.setDropDownViewResource(R.layout.spinner_item)
            adapter = subAdapter
        }

        val titleEt = EditText(this).apply { hint = "Assignment Topic (e.g. Lab 1)" }
        val deadlineEt = EditText(this).apply { hint = "Deadline (e.g. 30th Oct)" }
        
        val selectDocBtn = Button(this).apply {
            text = "Select Document (PDF/DOC)"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        
        docStatusTv = TextView(this).apply {
            text = "No document selected"
            textSize = 12f
            setPadding(0, 10, 0, 20)
        }

        selectDocBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" 
            }
            docPickerLauncher.launch(intent)
        }

        layout.addView(TextView(this).apply { text = "Select Subject:"; setPadding(0, 10, 0, 5) })
        layout.addView(subjectSpinner)
        layout.addView(titleEt)
        layout.addView(deadlineEt)
        layout.addView(selectDocBtn)
        layout.addView(docStatusTv)
        builder.setView(layout)

        builder.setPositiveButton("Upload") { _, _ ->
            val t = titleEt.text.toString()
            val d = deadlineEt.text.toString()
            val s = subjectSpinner.selectedItem.toString()
            if (t.isNotEmpty() && d.isNotEmpty() && selectedDocUri != null) {
                saveAssignment(t, d, selectedDocUri!!, s, currentFacultyId)
                NotificationHelper.sendNotification(
                    this, 
                    "📝 New Assignment: $s", 
                    "Topic: $t | Deadline: $d",
                    AssignmentActivity::class.java
                )
                loadAssignments()
            } else {
                Toast.makeText(this, "All fields and document required", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveAssignment(title: String, deadline: String, docUri: Uri, subject: String, facultyId: String) {
        val sharedPref = getSharedPreferences("AssignmentData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val id = UUID.randomUUID().toString()
        
        try {
            contentResolver.takePersistableUriPermission(docUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {}

        editor.putString("title_$id", title)
        editor.putString("deadline_$id", deadline)
        editor.putString("doc_$id", docUri.toString())
        editor.putString("subject_$id", subject)
        editor.putString("facultyId_$id", facultyId)
        
        val ids = sharedPref.getStringSet("assign_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = ids.toMutableSet()
        updatedIds.add(id)
        editor.putStringSet("assign_ids", updatedIds)
        editor.apply()
        Toast.makeText(this, "Assignment uploaded successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun showEditDialog(assign: Assignment) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Assignment Title")
        val titleEt = EditText(this).apply { setText(assign.title) }
        builder.setView(titleEt)
        builder.setPositiveButton("Update") { _, _ ->
            getSharedPreferences("AssignmentData", Context.MODE_PRIVATE).edit()
                .putString("title_${assign.id}", titleEt.text.toString()).apply()
            loadAssignments()
        }
        builder.show()
    }

    private fun deleteAssignment(assign: Assignment) {
        val sharedPref = getSharedPreferences("AssignmentData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val ids = sharedPref.getStringSet("assign_ids", mutableSetOf())?.toMutableSet()
        ids?.remove(assign.id)
        editor.putStringSet("assign_ids", ids)
        editor.remove("title_${assign.id}")
        editor.remove("deadline_${assign.id}")
        editor.remove("doc_${assign.id}")
        editor.remove("subject_${assign.id}")
        editor.remove("facultyId_${assign.id}")
        editor.apply()
        loadAssignments()
    }

    private fun viewSubmissions(assign: Assignment) {
        val sharedPref = getSharedPreferences("SubmissionData", Context.MODE_PRIVATE)
        val submissionList = sharedPref.getStringSet("list_${assign.id}", setOf()) ?: setOf()
        
        if (submissionList.isEmpty()) {
            Toast.makeText(this, "No submissions yet for ${assign.title}", Toast.LENGTH_LONG).show()
            return
        }

        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val details = StringBuilder()
        details.append("Students who submitted:\n\n")
        
        for (studentId in submissionList) {
            val name = userPref.getString("name_$studentId", "Unknown Student")
            details.append("• $name (ID: $studentId)\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Submissions: ${assign.title}")
            .setMessage(details.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    data class Assignment(val id: String, val title: String, val deadline: String, val docUri: String?, val subject: String)

    class AssignmentAdapter(
        private val list: List<Assignment>, 
        val onEdit: (Assignment) -> Unit, 
        val onDelete: (Assignment) -> Unit,
        val onView: (Assignment) -> Unit
    ) : RecyclerView.Adapter<AssignmentAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val titleTv: TextView = v.findViewById(R.id.assignTitleTv)
            val deadTv: TextView = v.findViewById(R.id.assignDeadlineTv)
            val editBtn: Button = v.findViewById(R.id.editAssignBtn)
            val deleteBtn: ImageButton = v.findViewById(R.id.deleteAssignBtn)
            val viewBtn: Button = v.findViewById(R.id.viewSubmissionsBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_faculty_assignment, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val a = list[position]
            h.titleTv.text = "[${a.subject}] ${a.title}"
            h.deadTv.text = "Deadline: ${a.deadline}"
            
            val subPref = h.itemView.context.getSharedPreferences("SubmissionData", Context.MODE_PRIVATE)
            val count = subPref.getStringSet("list_${a.id}", setOf())?.size ?: 0
            h.viewBtn.text = "Submissions ($count)"
            
            h.editBtn.setOnClickListener { onEdit(a) }
            h.deleteBtn.setOnClickListener { onDelete(a) }
            h.viewBtn.setOnClickListener { onView(a) }
        }
        override fun getItemCount() = list.size
    }
}
