package com.example.my_project

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    }

    private fun loadSchedule() {
        val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
        val lectureIds = sharedPref.getStringSet("lecture_ids", mutableSetOf()) ?: mutableSetOf()
        
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentFacultyId = userPref.getString("current_userId", "") ?: ""

        lectureList.clear()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())

        for (id in lectureIds) {
            val facultyId = sharedPref.getString("lec_fac_$id", "")
            if (facultyId == currentFacultyId) {
                val sub = sharedPref.getString("lec_sub_$id", "") ?: ""
                val time = sharedPref.getString("lec_time_$id", "") ?: ""
                val details = sharedPref.getString("lec_det_$id", "") ?: ""
                val dateStr = sharedPref.getString("lec_date_$id", todayStr) ?: todayStr
                val img = sharedPref.getString("lec_img_$id", null)
                val sem = sharedPref.getString("lec_sem_$id", "All") ?: "All"
                lectureList.add(Lecture(id, sub, time, details, dateStr, img, sem))
            }
        }
        
        lectureList.sortBy { it.date }
        adapter = TimetableAdapter(lectureList) { lec -> viewScheduleImage(lec) }
        recyclerView.adapter = adapter
    }

    private fun viewScheduleImage(lec: Lecture) {
        if (lec.imageUri == null) return
        val iv = ImageView(this).apply {
            setImageURI(Uri.parse(lec.imageUri))
            adjustViewBounds = true
        }
        AlertDialog.Builder(this).setView(iv).setPositiveButton("Close", null).show()
    }

    data class Lecture(val id: String, val subject: String, val time: String, val details: String, val date: String, val imageUri: String?, val semester: String)

    class TimetableAdapter(private val list: List<Lecture>, val onViewImg: (Lecture) -> Unit) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subTv: TextView = v.findViewById(R.id.subjectTv)
            val timeTv: TextView = v.findViewById(R.id.timeTv)
            val detTv: TextView = v.findViewById(R.id.detailsTv)
            val viewBtn: Button = v.findViewById(R.id.viewImgBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_faculty_timetable, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val l = list[position]
            h.subTv.text = "${l.subject} (${l.date}) - ${l.semester}"
            h.timeTv.text = l.time
            h.detTv.text = l.details
            
            // Hide management buttons for Faculty
            h.itemView.findViewById<Button>(R.id.rescheduleBtn)?.visibility = View.GONE
            h.itemView.findViewById<Button>(R.id.cancelBtn)?.visibility = View.GONE
            
            h.viewBtn.visibility = if (l.imageUri != null) View.VISIBLE else View.GONE
            h.viewBtn.setOnClickListener { onViewImg(l) }
        }
        override fun getItemCount() = list.size
    }
}
