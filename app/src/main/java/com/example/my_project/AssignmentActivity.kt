package com.example.my_project

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AssignmentActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentAssignmentAdapter
    private var assignmentList = mutableListOf<Assignment>()
    private var pendingAssignmentId: String? = null
    private var pendingPosition: Int = -1

    // File Picker Launcher
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val fileUri = result.data?.data
            val assignId = pendingAssignmentId
            
            if (fileUri != null && assignId != null) {
                completeSubmission(assignId, fileUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_assignment)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        backBtn?.setOnClickListener { finish() }

        recyclerView = findViewById(R.id.studentAssignmentRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadAssignments()
    }

    private fun loadAssignments() {
        val sharedPref = getSharedPreferences("AssignmentData", Context.MODE_PRIVATE)
        val assignIds = sharedPref.getStringSet("assign_ids", setOf()) ?: setOf()
        
        assignmentList.clear()
        for (id in assignIds) {
            val title = sharedPref.getString("title_$id", "") ?: ""
            val deadline = sharedPref.getString("deadline_$id", "") ?: ""
            if (title.isNotEmpty()) {
                assignmentList.add(Assignment(id, title, deadline))
            }
        }
        
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = userPref.getString("current_userId", "") ?: ""
        
        adapter = StudentAssignmentAdapter(assignmentList, currentId) { id, pos ->
            pendingAssignmentId = id
            pendingPosition = pos
            openFilePicker()
        }
        recyclerView.adapter = adapter
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Allow all file types (PDF, Images, etc.)
        }
        filePickerLauncher.launch(intent)
    }

    private fun completeSubmission(assignId: String, fileUri: Uri) {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val studentId = userPref.getString("current_userId", "") ?: ""
        val subPref = getSharedPreferences("SubmissionData", Context.MODE_PRIVATE)
        val editor = subPref.edit()

        // Persist permission for the file so faculty can potentially see it (simulated)
        try {
            contentResolver.takePersistableUriPermission(fileUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {}

        editor.putBoolean("sub_${assignId}_$studentId", true)
        editor.putString("file_${assignId}_$studentId", fileUri.toString())

        val submissionList = subPref.getStringSet("list_$assignId", mutableSetOf()) ?: mutableSetOf()
        val updatedList = submissionList.toMutableSet()
        updatedList.add(studentId)
        editor.putStringSet("list_$assignId", updatedList)
        editor.apply()

        Toast.makeText(this, "File submitted successfully!", Toast.LENGTH_SHORT).show()
        adapter.notifyItemChanged(pendingPosition)
    }

    data class Assignment(val id: String, val title: String, val deadline: String)

    class StudentAssignmentAdapter(
        private val list: List<Assignment>, 
        private val studentId: String,
        val onUploadClick: (String, Int) -> Unit
    ) : RecyclerView.Adapter<StudentAssignmentAdapter.ViewHolder>() {
        
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val titleTv: TextView = v.findViewById(R.id.studentAssignTitleTv)
            val deadTv: TextView = v.findViewById(R.id.studentAssignDeadlineTv)
            val submitBtn: MaterialButton = v.findViewById(R.id.submitAssignBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_student_assignment, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val a = list[position]
            h.titleTv.text = a.title
            h.deadTv.text = "Deadline: ${a.deadline}"
            
            val subPref = h.itemView.context.getSharedPreferences("SubmissionData", Context.MODE_PRIVATE)
            val isSubmitted = subPref.getBoolean("sub_${a.id}_$studentId", false)
            
            if (isSubmitted) {
                h.submitBtn.text = "Submitted ✅"
                h.submitBtn.isEnabled = false
            } else {
                h.submitBtn.text = "Select File & Submit"
                h.submitBtn.setOnClickListener { onUploadClick(a.id, position) }
            }
        }
        override fun getItemCount() = list.size
    }
}
