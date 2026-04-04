package com.example.my_project

import android.app.Activity
import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.*

class FacultyTimetableActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TimetableAdapter
    private var lectureList = mutableListOf<Lecture>()
    
    private var selectedImageUri: Uri? = null
    private var imageStatusTv: TextView? = null

    // Launcher for selecting Schedule Image
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            imageStatusTv?.text = "📸 Image Selected"
            imageStatusTv?.setTextColor(resources.getColor(R.color.success))
        }
    }

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
                
                if (!lecDate.before(today)) {
                    val subject = sharedPref.getString("lec_sub_$id", "") ?: ""
                    val time = sharedPref.getString("lec_time_$id", "") ?: ""
                    val details = sharedPref.getString("lec_det_$id", "") ?: ""
                    val img = sharedPref.getString("lec_img_$id", null)
                    lectureList.add(Lecture(id, subject, time, details, dateStr, img))
                }
            }
        }
        
        lectureList.sortWith(compareBy({ it.date }, { it.time }))
        adapter = TimetableAdapter(lectureList, 
            { lec -> showRescheduleDialog(lec) }, 
            { lec -> showCancelDialog(lec) },
            { lec -> viewScheduleImage(lec) }
        )
        recyclerView.adapter = adapter
    }

    private fun showAddExtraDialog() {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = userPref.getString("current_userId", "") ?: ""
        
        val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val allocatedSubjects = allocPref.getStringSet("subjects_$currentId", setOf())?.toList() ?: listOf()

        if (allocatedSubjects.isEmpty()) {
            Toast.makeText(this, "No subjects allocated to you", Toast.LENGTH_SHORT).show()
            return
        }

        selectedImageUri = null
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Upload New Schedule")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val subSpinner = Spinner(this).apply {
            val subAdapter = ArrayAdapter(context, R.layout.spinner_item, allocatedSubjects)
            subAdapter.setDropDownViewResource(R.layout.spinner_item)
            adapter = subAdapter
        }

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

        val timeEt = EditText(this).apply { hint = "Time (e.g. 10:00 AM)" }
        val detailEt = EditText(this).apply { hint = "Details (Batch/Room)" }
        
        val selectImgBtn = Button(this).apply { text = "📸 Select Schedule Image" }
        imageStatusTv = TextView(this).apply { text = "Optional: Upload image of schedule"; setPadding(0, 10, 0, 20) }

        selectImgBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        layout.addView(TextView(this).apply { text = "Subject:" })
        layout.addView(subSpinner)
        layout.addView(dateBtn)
        layout.addView(timeEt)
        layout.addView(detailEt)
        layout.addView(selectImgBtn)
        layout.addView(imageStatusTv)

        builder.setView(layout)
        builder.setPositiveButton("Add") { _, _ ->
            val s = subSpinner.selectedItem.toString()
            val t = timeEt.text.toString()
            val d = detailEt.text.toString()
            if (s.isNotEmpty() && t.isNotEmpty()) {
                saveLecture(s, t, d, currentId, selectedDate, selectedImageUri?.toString())
                loadSchedule()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
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

    private fun saveLecture(sub: String, time: String, det: String, facultyId: String, date: String, imgUri: String?) {
        val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val id = UUID.randomUUID().toString()
        
        editor.putString("lec_sub_$id", sub)
        editor.putString("lec_time_$id", time)
        editor.putString("lec_det_$id", det)
        editor.putString("lec_fac_$id", facultyId)
        editor.putString("lec_date_$id", date)
        if (imgUri != null) {
            editor.putString("lec_img_$id", imgUri)
            try { contentResolver.takePersistableUriPermission(Uri.parse(imgUri), Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
        }
        
        val ids = sharedPref.getStringSet("lecture_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = ids.toMutableSet()
        updatedIds.add(id)
        editor.putStringSet("lecture_ids", updatedIds)
        editor.apply()
    }

    private fun showRescheduleDialog(lec: Lecture) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reschedule Class")
        val timeEt = EditText(this).apply { hint = "New Time"; setText(lec.time) }
        builder.setView(timeEt)
        builder.setPositiveButton("Update") { _, _ ->
            val newTime = timeEt.text.toString()
            if (newTime.isNotEmpty()) {
                getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE).edit().putString("lec_time_${lec.id}", newTime).apply()
                loadSchedule()
            }
        }
        builder.show()
    }

    private fun showCancelDialog(lec: Lecture) {
        AlertDialog.Builder(this).setTitle("Cancel Lecture").setMessage("Are you sure?")
            .setPositiveButton("Confirm") { _, _ ->
                val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                val ids = sharedPref.getStringSet("lecture_ids", mutableSetOf())?.toMutableSet()
                ids?.remove(lec.id)
                editor.putStringSet("lecture_ids", ids)
                editor.apply()
                loadSchedule()
            }.show()
    }

    data class Lecture(val id: String, val subject: String, var time: String, val details: String, val date: String, val imageUri: String?)

    class TimetableAdapter(
        private val list: List<Lecture>, 
        val onResched: (Lecture) -> Unit, 
        val onCancel: (Lecture) -> Unit,
        val onViewImg: (Lecture) -> Unit
    ) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val subTv: TextView = v.findViewById(R.id.subjectTv)
            val timeTv: TextView = v.findViewById(R.id.timeTv)
            val detTv: TextView = v.findViewById(R.id.detailsTv)
            val reschedBtn: Button = v.findViewById(R.id.rescheduleBtn)
            val cancelBtn: Button = v.findViewById(R.id.cancelBtn)
            val viewBtn: Button = Button(v.context).apply { 
                text = "View Image"
                visibility = View.GONE
            }
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_faculty_timetable, parent, false)
            val vh = ViewHolder(v)
            (v as ViewGroup).addView(vh.viewBtn) // Temporary dynamic add
            return vh
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val l = list[position]
            h.subTv.text = "${l.subject} (${l.date})"
            h.timeTv.text = l.time
            h.detTv.text = l.details
            
            h.viewBtn.visibility = if (l.imageUri != null) View.VISIBLE else View.GONE
            h.viewBtn.setOnClickListener { onViewImg(l) }
            
            h.reschedBtn.setOnClickListener { onResched(l) }
            h.cancelBtn.setOnClickListener { onCancel(l) }
        }
        override fun getItemCount() = list.size
    }
}
