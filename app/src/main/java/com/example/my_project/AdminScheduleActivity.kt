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

class AdminScheduleActivity : AppCompatActivity() {

    private lateinit var facultySpinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TimetableAdapter
    private var lectureList = mutableListOf<Lecture>()
    private var facultyList = mutableListOf<Faculty>()
    
    private var selectedImageUri: Uri? = null
    private var imageStatusTv: TextView? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            selectedImageUri = result.data?.data
            imageStatusTv?.text = "📸 Image Selected"
            imageStatusTv?.setTextColor(resources.getColor(R.color.success))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_schedule)

        facultySpinner = findViewById(R.id.facultySpinner)
        recyclerView = findViewById(R.id.scheduleRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupFacultySpinner()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.addScheduleBtn).setOnClickListener { 
            if (facultySpinner.selectedItemPosition > 0) {
                showAddDialog()
            } else {
                Toast.makeText(this, "Please select a faculty first", Toast.LENGTH_SHORT).show()
            }
        }

        facultySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                if (position > 0) {
                    loadSchedule(facultyList[position - 1].id)
                } else {
                    lectureList.clear()
                    if (::adapter.isInitialized) adapter.notifyDataSetChanged()
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun setupFacultySpinner() {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val allIds = sharedPref.getStringSet("all_user_ids", setOf()) ?: setOf()
        
        facultyList.clear()
        val facultyNames = mutableListOf<String>()
        facultyNames.add("All Faculty")

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
    }

    private fun loadSchedule(facultyId: String) {
        val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
        val lectureIds = sharedPref.getStringSet("lecture_ids", mutableSetOf()) ?: mutableSetOf()
        
        lectureList.clear()
        for (id in lectureIds) {
            val fId = sharedPref.getString("lec_fac_$id", "")
            if (fId == facultyId) {
                val sub = sharedPref.getString("lec_sub_$id", "") ?: ""
                val time = sharedPref.getString("lec_time_$id", "") ?: ""
                val det = sharedPref.getString("lec_det_$id", "") ?: ""
                val date = sharedPref.getString("lec_date_$id", "") ?: ""
                val img = sharedPref.getString("lec_img_$id", null)
                val sem = sharedPref.getString("lec_sem_$id", "All") ?: "All"
                lectureList.add(Lecture(id, sub, time, det, date, img, sem))
            }
        }
        
        lectureList.sortByDescending { it.date }
        adapter = TimetableAdapter(lectureList, { lec -> deleteSchedule(lec) }, { lec -> viewImage(lec) })
        recyclerView.adapter = adapter
    }

    private fun showAddDialog() {
        val faculty = facultyList[facultySpinner.selectedItemPosition - 1]
        
        // Get allocated subjects for this faculty
        val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
        val allAllocIds = allocPref.getStringSet("allocation_ids", setOf()) ?: setOf()
        val facultyAllocations = mutableListOf<Pair<String, String>>()
        for (allocId in allAllocIds) {
            if (allocPref.getString("fac_id_$allocId", "") == faculty.id) {
                val sub = allocPref.getString("subject_$allocId", "") ?: ""
                val sem = allocPref.getString("semester_$allocId", "") ?: ""
                facultyAllocations.add(sub to sem)
            }
        }

        if (facultyAllocations.isEmpty()) {
            Toast.makeText(this, "This faculty has no subjects allocated.", Toast.LENGTH_SHORT).show()
            return
        }

        val displayList = facultyAllocations.map { "${it.first} (${it.second})" }

        selectedImageUri = null
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Upload New Schedule")

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val subSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, displayList)
        }

        val dateBtn = Button(this).apply { text = "SELECT DATE" }
        var selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        dateBtn.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d)
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(c.time)
                dateBtn.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val timeEt = EditText(this).apply { hint = "Time (e.g. 10:00 AM)" }
        val detailEt = EditText(this).apply { hint = "Details (Batch/Room)" }
        
        val selectImgBtn = Button(this).apply { text = "📸 SELECT SCHEDULE IMAGE" }
        imageStatusTv = TextView(this).apply { text = "Optional: Upload image of schedule"; setPadding(0, 10, 0, 20) }

        selectImgBtn.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        layout.addView(TextView(this).apply { text = "Subject & Semester:" })
        layout.addView(subSpinner)
        layout.addView(dateBtn)
        layout.addView(timeEt)
        layout.addView(detailEt)
        layout.addView(selectImgBtn)
        layout.addView(imageStatusTv)

        builder.setView(layout)
        builder.setPositiveButton("ADD") { _, _ ->
            val alloc = facultyAllocations[subSpinner.selectedItemPosition]
            val t = timeEt.text.toString()
            val d = detailEt.text.toString()
            if (t.isNotEmpty()) {
                saveSchedule(alloc.first, t, d, faculty.id, selectedDate, selectedImageUri?.toString(), alloc.second)
                loadSchedule(faculty.id)
            }
        }
        builder.setNegativeButton("CANCEL", null)
        builder.show()
    }

    private fun saveSchedule(sub: String, time: String, det: String, facultyId: String, date: String, imgUri: String?, sem: String) {
        val sharedPref = getSharedPreferences("FacultySchedule", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val id = UUID.randomUUID().toString()
        
        editor.putString("lec_sub_$id", sub)
        editor.putString("lec_time_$id", time)
        editor.putString("lec_det_$id", det)
        editor.putString("lec_fac_$id", facultyId)
        editor.putString("lec_date_$id", date)
        editor.putString("lec_sem_$id", sem)
        if (imgUri != null) {
            editor.putString("lec_img_$id", imgUri)
            try { contentResolver.takePersistableUriPermission(Uri.parse(imgUri), Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (e: Exception) {}
        }
        
        val ids = sharedPref.getStringSet("lecture_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = ids.toMutableSet()
        updatedIds.add(id)
        editor.putStringSet("lecture_ids", updatedIds)
        editor.apply()
        Toast.makeText(this, "Schedule added successfully", Toast.LENGTH_SHORT).show()
    }

    private fun deleteSchedule(lec: Lecture) {
        AlertDialog.Builder(this).setTitle("Delete Schedule").setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
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
                editor.remove("lec_sem_${lec.id}")
                editor.remove("lec_img_${lec.id}")
                editor.apply()
                loadSchedule(facultyList[facultySpinner.selectedItemPosition - 1].id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun viewImage(lec: Lecture) {
        if (lec.imageUri == null) return
        val iv = ImageView(this).apply { setImageURI(Uri.parse(lec.imageUri)); adjustViewBounds = true }
        AlertDialog.Builder(this).setView(iv).setPositiveButton("Close", null).show()
    }

    data class Faculty(val id: String, val name: String)
    data class Lecture(val id: String, val subject: String, val time: String, val details: String, val date: String, val imageUri: String?, val semester: String)

    class TimetableAdapter(private val list: List<Lecture>, val onDelete: (Lecture) -> Unit, val onView: (Lecture) -> Unit) : RecyclerView.Adapter<TimetableAdapter.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
            val title: TextView = v.findViewById(R.id.subjectTv)
            val time: TextView = v.findViewById(R.id.timeTv)
            val det: TextView = v.findViewById(R.id.detailsTv)
            val delBtn: Button = v.findViewById(R.id.cancelBtn) // Reusing faculty layout buttons
            val viewBtn: Button = v.findViewById(R.id.viewImgBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_faculty_timetable, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val item = list[position]
            h.title.text = "${item.subject} (${item.date}) - ${item.semester}"
            h.time.text = item.time
            h.det.text = item.details
            h.delBtn.text = "Delete"
            h.delBtn.setOnClickListener { onDelete(item) }
            h.viewBtn.visibility = if (item.imageUri != null) View.VISIBLE else View.GONE
            h.viewBtn.setOnClickListener { onView(item) }
            // Hide the reschedule button if it exists in layout
            h.itemView.findViewById<Button>(R.id.rescheduleBtn)?.visibility = View.GONE
        }
        override fun getItemCount() = list.size
    }
}
