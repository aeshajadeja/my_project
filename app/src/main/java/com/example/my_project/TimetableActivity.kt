package com.example.my_project

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        
        val currentId = userPref.getString("current_userId", "") ?: ""
        val studentSem = userPref.getString("current_sem_$currentId", "All") ?: "All"
        
        val lectureIds = sharedPref.getStringSet("lecture_ids", setOf()) ?: setOf()
        
        lectureList.clear()
        val tomorrowSummary = StringBuilder()

        for (id in lectureIds) {
            val subject = sharedPref.getString("lec_sub_$id", "") ?: ""
            val time = sharedPref.getString("lec_time_$id", "") ?: ""
            val details = sharedPref.getString("lec_det_$id", "") ?: ""
            val imageUri = sharedPref.getString("lec_img_$id", null)
            val lecSem = sharedPref.getString("lec_sem_$id", "All") ?: "All"
            
            if (subject.isNotEmpty()) {
                // Filter: Show if it's for everyone OR matches student's semester
                if (lecSem == "All" || lecSem == studentSem) {
                    lectureList.add(Lecture(id, subject, time, details, imageUri, lecSem))
                    tomorrowSummary.append("$subject ($time), ")
                }
            }
        }

        if (lectureList.isEmpty()) {
            tomorrowTv.text = "No classes scheduled yet for $studentSem."
        } else {
            tomorrowTv.text = tomorrowSummary.toString().removeSuffix(", ")
        }

        adapter = StudentTimetableAdapter(lectureList) { lecture ->
            viewScheduleImage(lecture)
        }
        recyclerView.adapter = adapter
    }

    private fun viewScheduleImage(lec: Lecture) {
        if (lec.imageUri == null) return
        val builder = AlertDialog.Builder(this)
        val iv = ImageView(this).apply {
            setImageURI(Uri.parse(lec.imageUri))
            adjustViewBounds = true
            setPadding(20, 20, 20, 20)
        }
        builder.setView(iv)
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    data class Lecture(val id: String, val subject: String, val time: String, val details: String, val imageUri: String?, val semester: String)

    class StudentTimetableAdapter(private val list: List<Lecture>, val onViewImg: (Lecture) -> Unit) : RecyclerView.Adapter<StudentTimetableAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subTv: TextView = v.findViewById(R.id.studentSubjectTv)
            val timeTv: TextView = v.findViewById(R.id.studentTimeTv)
            val detTv: TextView = v.findViewById(R.id.studentDetailsTv)
            val viewBtn: Button = v.findViewById(R.id.viewStudentImgBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_student_timetable, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val l = list[position]
            h.subTv.text = "${l.subject} (${l.semester})"
            h.timeTv.text = l.time
            h.detTv.text = l.details
            
            if (l.imageUri != null) {
                h.viewBtn.visibility = View.VISIBLE
                h.viewBtn.setOnClickListener { onViewImg(l) }
            } else {
                h.viewBtn.visibility = View.GONE
            }
        }
        override fun getItemCount() = list.size
    }
}
