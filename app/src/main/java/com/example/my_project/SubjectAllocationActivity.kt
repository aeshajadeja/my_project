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
import java.util.*

class SubjectAllocationActivity : AppCompatActivity() {

    private lateinit var facultySpinner: Spinner
    private lateinit var subjectSpinner: Spinner
    private lateinit var semesterSpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AllocationAdapter
    private var allocationList = mutableListOf<Allocation>()
    private var facultyList = mutableListOf<Faculty>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subject_allocation)

        facultySpinner = findViewById(R.id.facultySpinner)
        subjectSpinner = findViewById(R.id.subjectSpinner)
        semesterSpinner = findViewById(R.id.semesterSpinner)
        recyclerView = findViewById(R.id.allocationRecyclerView)
        val allocateBtn = findViewById<MaterialButton>(R.id.allocateBtn)
        val backBtn = findViewById<ImageButton>(R.id.backBtn)

        recyclerView.layoutManager = LinearLayoutManager(this)

        setupSpinners()
        loadAllocations()

        backBtn.setOnClickListener { finish() }

        allocateBtn.setOnClickListener {
            performAllocation()
        }
    }

    private fun setupSpinners() {
        // 1. Load Faculty
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val allIds = sharedPref.getStringSet("all_user_ids", setOf()) ?: setOf()
        
        facultyList.clear()
        val facultyNames = mutableListOf<String>()
        facultyNames.add("Select Faculty")

        for (id in allIds) {
            if (sharedPref.getString("role_$id", "") == "Faculty") {
                val name = sharedPref.getString("name_$id", "Faculty")!!
                facultyList.add(Faculty(id, name))
                facultyNames.add("$name ($id)")
            }
        }

        val facultyAdapter = ArrayAdapter(this, R.layout.spinner_item, facultyNames)
        facultyAdapter.setDropDownViewResource(R.layout.spinner_item)
        facultySpinner.adapter = facultyAdapter

        // 2. Load Subjects
        val subjects = arrayOf("Select Subject", "Java", "Python", "C++", "Maths", "DBMS", "Android", "Web Dev")
        val subAdapter = ArrayAdapter(this, R.layout.spinner_item, subjects)
        subAdapter.setDropDownViewResource(R.layout.spinner_item)
        subjectSpinner.adapter = subAdapter

        // 3. Load Semesters
        val sems = arrayOf("Select Semester", "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8")
        val semAdapter = ArrayAdapter(this, R.layout.spinner_item, sems)
        semAdapter.setDropDownViewResource(R.layout.spinner_item)
        semesterSpinner.adapter = semAdapter
    }

    private fun performAllocation() {
        if (facultySpinner.selectedItemPosition == 0 || 
            subjectSpinner.selectedItemPosition == 0 || 
            semesterSpinner.selectedItemPosition == 0) {
            Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val faculty = facultyList[facultySpinner.selectedItemPosition - 1]
        val subject = subjectSpinner.selectedItem.toString()
        val semester = semesterSpinner.selectedItem.toString()

        val sharedPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        
        val allocId = UUID.randomUUID().toString()
        editor.putString("fac_id_$allocId", faculty.id)
        editor.putString("fac_name_$allocId", faculty.name)
        editor.putString("subject_$allocId", subject)
        editor.putString("semester_$allocId", semester)

        val allAllocIds = sharedPref.getStringSet("allocation_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = allAllocIds.toMutableSet()
        updatedIds.add(allocId)
        editor.putStringSet("allocation_ids", updatedIds)

        // Store subjects specifically for this faculty for quick lookup
        val facultySubjects = sharedPref.getStringSet("subjects_${faculty.id}", mutableSetOf()) ?: mutableSetOf()
        val updatedSubSets = facultySubjects.toMutableSet()
        updatedSubSets.add(subject)
        editor.putStringSet("subjects_${faculty.id}", updatedSubSets)

        editor.apply()

        Toast.makeText(this, "Subject Allocated Successfully", Toast.LENGTH_SHORT).show()
        loadAllocations()
        
        // Reset selection
        facultySpinner.setSelection(0)
        subjectSpinner.setSelection(0)
        semesterSpinner.setSelection(0)
    }

    private fun loadAllocations() {
        val sharedPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val ids = sharedPref.getStringSet("allocation_ids", setOf()) ?: setOf()
        
        allocationList.clear()
        for (id in ids) {
            val fId = sharedPref.getString("fac_id_$id", "") ?: ""
            val fName = sharedPref.getString("fac_name_$id", "") ?: ""
            val sub = sharedPref.getString("subject_$id", "") ?: ""
            val sem = sharedPref.getString("semester_$id", "") ?: ""
            allocationList.add(Allocation(id, fId, fName, sub, sem))
        }
        
        adapter = AllocationAdapter(allocationList) { alloc -> deleteAllocation(alloc) }
        recyclerView.adapter = adapter
    }

    private fun deleteAllocation(alloc: Allocation) {
        AlertDialog.Builder(this)
            .setTitle("Remove Allocation")
            .setMessage("Remove ${alloc.subject} from ${alloc.facultyName}?")
            .setPositiveButton("Remove") { _, _ ->
                val sharedPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                
                val ids = sharedPref.getStringSet("allocation_ids", mutableSetOf())?.toMutableSet()
                ids?.remove(alloc.id)
                editor.putStringSet("allocation_ids", ids)
                
                // Also update the subjects set for the faculty
                val facultySubjects = sharedPref.getStringSet("subjects_${alloc.facultyId}", mutableSetOf())?.toMutableSet()
                facultySubjects?.remove(alloc.subject)
                editor.putStringSet("subjects_${alloc.facultyId}", facultySubjects)

                editor.remove("fac_id_${alloc.id}")
                editor.remove("fac_name_${alloc.id}")
                editor.remove("subject_${alloc.id}")
                editor.remove("semester_${alloc.id}")
                editor.apply()
                
                loadAllocations()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    data class Faculty(val id: String, val name: String)
    data class Allocation(val id: String, val facultyId: String, val facultyName: String, val subject: String, val semester: String)

    class AllocationAdapter(private val list: List<Allocation>, val onDelete: (Allocation) -> Unit) : RecyclerView.Adapter<AllocationAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val titleTv: TextView = v.findViewById(R.id.allocTitleTv)
            val detailTv: TextView = v.findViewById(R.id.allocDetailTv)
            val deleteBtn: ImageButton = v.findViewById(R.id.deleteAllocBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_allocation, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val item = list[position]
            h.titleTv.text = "${item.subject} - ${item.semester}"
            h.detailTv.text = "Faculty: ${item.facultyName} (${item.facultyId})"
            h.deleteBtn.setOnClickListener { onDelete(item) }
        }
        override fun getItemCount() = list.size
    }
}
