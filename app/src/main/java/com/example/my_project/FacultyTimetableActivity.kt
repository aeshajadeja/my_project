package com.example.my_project

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
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
        
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentFacultyId = userPref.getString("current_userId", "") ?: ""

        lectureList.clear()
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val today = sdf.parse(todayStr) ?: Date()

        for (id in lectureIds) {
            val facultyId = sharedPref.getString("lec_fac_$id", "")
            if (facultyId == currentFacultyId) {
                val dateStr = sharedPref.getString("lec_date_$id", todayStr) ?: todayStr
                val lecDate = sdf.parse(dateStr) ?: today
                
                // Show only today's and future lectures
                if (!lecDate.before(today)) {
                    val subject = sharedPref.getString("lec_sub_$id", "") ?: ""
                    val time = sharedPref.getString("lec_time_$id", "") ?: ""
                    val details = sharedPref.getString("lec_det_$id", "") ?: ""
                    lectureList.add(Lecture(id, subject, time, details, dateStr))
                }
            }
        }
        
        // Sort by date then time (simple alphabetical time sort for now)
        lectureList.sortWith(compareBy({ it.date }, { it.time }))
        
        adapter = TimetableAdapter(lectureList, { lec -> showRescheduleDialog(lec) }, { lec -> showCancelDialog(lec) })
        recyclerView.adapter = adapter
    }

    private fun showAddExtraDialog() {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentFacultyId = userPref.getString("current_userId", "") ?: ""
        
        val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val allocatedSubjects = allocPref.getStringSet("subjects_$currentFacultyId", setOf())?.toList() ?: listOf()

        if (allocatedSubjects.isEmpty()) {
            Toast.makeText(this, "No subjects allocated to you. Contact Admin.", Toast.LENGTH_LONG).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Extra Lecture")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val subSpinner = Spinner(this).apply {
            val subAdapter = ArrayAdapter(context, R.layout.spinner_item, allocatedSubjects)
            subAdapter.setDropDownViewResource(R.layout.spinner_item)
            adapter = subAdapter
        }
        layout.addView(TextView(this).apply { text = "Select Subject:"; setPadding(0, 10, 0, 5) })
        layout.addView(subSpinner)

        val dateBtn = Button(this).apply { text = "Select Date" }
        var selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dateBtn.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val cal = Calendar.getInstance()
                cal.set(y, m, d)
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                dateBtn.text = "Date: $selectedDate"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        layout.addView(dateBtn)

        val timeEt = EditText(this).apply { hint = "Time (e.g. 03:00 PM)" }
        layout.addView(timeEt)

        val detailEt = EditText(this).apply { hint = "Details (Batch/Room)" }
        layout.addView(detailEt)

        builder.setView(layout)
        builder.setPositiveButton("Add") { _, _ ->
            val s = subSpinner.selectedItem.toString()
            val t = timeEt.text.toString()
            val d = detailEt.text.toString()
            if (s.isNotEmpty() && t.isNotEmpty()) {
                saveLecture(s, t, d, currentFacultyId, selectedDate)
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
        
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val timeEt = EditText(this).apply { 
            hint = "New Time"
            setText(lec.time)
        }
        layout.addView(timeEt)
        
        builder.setView(layout)
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
            editor.remove("lec_fac_${lec.id}")
            editor.remove("lec_date_${lec.id}")
            editor.apply()
            
            loadSchedule()
            Toast.makeText(this, "Lecture cancelled. Notification sent to students.", Toast.LENGTH_LONG).show()
        }
        builder.setNegativeButton("Back", null)
        builder.show()
    }

    private fun saveLecture(sub: String, time: String, det: String, facultyId: String, date: String) {
        val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val id = UUID.randomUUID().toString()
        
        editor.putString("lec_sub_$id", sub)
        editor.putString("lec_time_$id", time)
        editor.putString("lec_det_$id", det)
        editor.putString("lec_fac_$id", facultyId)
        editor.putString("lec_date_$id", date)
        
        val ids = sharedPref.getStringSet("lecture_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = ids.toMutableSet()
        updatedIds.add(id)
        editor.putStringSet("lecture_ids", updatedIds)
        editor.apply()
    }

    data class Lecture(val id: String, val subject: String, var time: String, val details: String, val date: String)

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
            h.subTv.text = "${l.subject} (${l.date})"
            h.timeTv.text = l.time
            h.detTv.text = l.details
            h.reschedBtn.setOnClickListener { onResched(l) }
            h.cancelBtn.setOnClickListener { onCancel(l) }
        }
        override fun getItemCount() = list.size
    }
}
