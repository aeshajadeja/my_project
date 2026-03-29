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

class FacultyTimetableActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TimetableAdapter
    private var lectureList = mutableListOf<Lecture>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faculty_timetable)

        recyclerView = findViewById(R.id.facultyTimetableRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadSchedule()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.addExtraLectureBtn).setOnClickListener { showAddExtraDialog() }
    }

    private fun loadSchedule() {
        val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
        val lectureIds = sharedPref.getStringSet("lecture_ids", mutableSetOf()) ?: mutableSetOf()
        
        lectureList.clear()
        
        // Add some default mock data if empty for the first time
        if (lectureIds.isEmpty()) {
            addDefaultLectures()
        } else {
            for (id in lectureIds) {
                val subject = sharedPref.getString("lec_sub_$id", "") ?: ""
                val time = sharedPref.getString("lec_time_$id", "") ?: ""
                val details = sharedPref.getString("lec_det_$id", "") ?: ""
                lectureList.add(Lecture(id, subject, time, details))
            }
        }
        
        adapter = TimetableAdapter(lectureList, { lec -> showRescheduleDialog(lec) }, { lec -> showCancelDialog(lec) })
        recyclerView.adapter = adapter
    }

    private fun addDefaultLectures() {
        saveLecture("Java Programming", "10:30 AM", "Batch: 2023-2026 | Room: 402")
        saveLecture("Database Management", "01:15 PM", "Batch: 2023-2026 | Room: 101")
    }

    private fun showAddExtraDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Extra Lecture")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val subEt = EditText(this).apply { hint = "Subject Name" }
        layout.addView(subEt)

        val timeEt = EditText(this).apply { hint = "Time (e.g. 03:00 PM)" }
        layout.addView(timeEt)

        val detailEt = EditText(this).apply { hint = "Details (Batch/Room)" }
        layout.addView(detailEt)

        builder.setView(layout)
        builder.setPositiveButton("Add") { _, _ ->
            val s = subEt.text.toString()
            val t = timeEt.text.toString()
            val d = detailEt.text.toString()
            if (s.isNotEmpty() && t.isNotEmpty()) {
                saveLecture(s, t, d)
                Toast.makeText(this, "Extra lecture added", Toast.LENGTH_SHORT).show()
                loadSchedule()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showRescheduleDialog(lec: Lecture) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reschedule Class")
        
        val timeEt = EditText(this).apply { 
            hint = "New Time"
            setText(lec.time)
        }
        
        builder.setView(timeEt)
        builder.setPositiveButton("Update") { _, _ ->
            val newTime = timeEt.text.toString()
            if (newTime.isNotEmpty()) {
                val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
                sharedPref.edit().putString("lec_time_${lec.id}", newTime).apply()
                loadSchedule()
                Toast.makeText(this, "Schedule updated", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Back", null)
        builder.show()
    }

    private fun showCancelDialog(lec: Lecture) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cancel Lecture")
        builder.setMessage("Enter reason for cancellation:")

        val reasonEt = EditText(this).apply { hint = "Reason (e.g. Health issue)" }
        builder.setView(reasonEt)

        builder.setPositiveButton("Confirm Cancel") { _, _ ->
            val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            
            val ids = sharedPref.getStringSet("lecture_ids", mutableSetOf())?.toMutableSet()
            ids?.remove(lec.id)
            editor.putStringSet("lecture_ids", ids)
            
            editor.remove("lec_sub_${lec.id}")
            editor.remove("lec_time_${lec.id}")
            editor.remove("lec_det_${lec.id}")
            editor.apply()
            
            loadSchedule()
            Toast.makeText(this, "Lecture cancelled. Notification sent to students.", Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton("Back", null)
        builder.show()
    }

    private fun saveLecture(sub: String, time: String, det: String) {
        val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val id = UUID.randomUUID().toString()
        
        editor.putString("lec_sub_$id", sub)
        editor.putString("lec_time_$id", time)
        editor.putString("lec_det_$id", det)
        
        val ids = sharedPref.getStringSet("lecture_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = ids.toMutableSet()
        updatedIds.add(id)
        editor.putStringSet("lecture_ids", updatedIds)
        editor.apply()
    }

    data class Lecture(val id: String, val subject: String, var time: String, val details: String)

    class TimetableAdapter(private val list: List<Lecture>, val onResched: (Lecture) -> Unit, val onCancel: (Lecture) -> Unit) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subTv: TextView = v.findViewById(R.id.subjectTv)
            val timeTv: TextView = v.findViewById(R.id.timeTv)
            val detTv: TextView = v.findViewById(R.id.detailsTv)
            val reschedBtn: Button = v.findViewById(R.id.rescheduleBtn)
            val cancelBtn: Button = v.findViewById(R.id.cancelBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_faculty_timetable, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val l = list[position]
            h.subTv.text = l.subject
            h.timeTv.text = l.time
            h.detTv.text = l.details
            h.reschedBtn.setOnClickListener { onResched(l) }
            h.cancelBtn.setOnClickListener { onCancel(l) }
        }
        override fun getItemCount() = list.size
    }
}
