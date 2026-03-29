package com.example.my_project

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class TimetableActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StudentTimetableAdapter
    private var lectureList = mutableListOf<Lecture>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timetable)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        val tomorrowSummaryTv = findViewById<TextView>(R.id.tomorrowSummaryTv)

        recyclerView = findViewById(R.id.studentTimetableRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        backBtn.setOnClickListener { finish() }

        loadSchedule(tomorrowSummaryTv)
    }

    private fun loadSchedule(tomorrowTv: TextView) {
        val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
        val lectureIds = sharedPref.getStringSet("lecture_ids", setOf()) ?: setOf()
        
        lectureList.clear()
        val tomorrowSummary = StringBuilder()

        for (id in lectureIds) {
            val subject = sharedPref.getString("lec_sub_$id", "") ?: ""
            val time = sharedPref.getString("lec_time_$id", "") ?: ""
            val details = sharedPref.getString("lec_det_$id", "") ?: ""
            
            if (subject.isNotEmpty()) {
                lectureList.add(Lecture(id, subject, time, details))
                tomorrowSummary.append("$subject ($time), ")
            }
        }

        // Logic for Tomorrow's Summary
        if (lectureList.isEmpty()) {
            tomorrowTv.text = "No classes scheduled yet."
        } else {
            tomorrowTv.text = tomorrowSummary.toString().removeSuffix(", ")
        }

        adapter = StudentTimetableAdapter(lectureList)
        recyclerView.adapter = adapter
    }

    data class Lecture(val id: String, val subject: String, val time: String, val details: String)

    class StudentTimetableAdapter(private val list: List<Lecture>) : RecyclerView.Adapter<StudentTimetableAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subTv: TextView = v.findViewById(R.id.studentSubjectTv)
            val timeTv: TextView = v.findViewById(R.id.studentTimeTv)
            val detTv: TextView = v.findViewById(R.id.studentDetailsTv)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_student_timetable, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val l = list[position]
            h.subTv.text = l.subject
            h.timeTv.text = l.time
            h.detTv.text = l.details
        }
        override fun getItemCount() = list.size
    }
}
