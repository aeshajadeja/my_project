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
import java.text.SimpleDateFormat
import java.util.*

class NoticeManagementActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoticeAdapter
    private var noticeList = mutableListOf<Notice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice_management)

        recyclerView = findViewById(R.id.noticeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadNotices()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }
        findViewById<MaterialButton>(R.id.addNoticeBtn).setOnClickListener { showAddNoticeDialog() }
    }

    private fun loadNotices() {
        val sharedPref = getSharedPreferences("CampusData", Context.MODE_PRIVATE)
        val noticeIds = sharedPref.getStringSet("notice_ids", mutableSetOf()) ?: mutableSetOf()
        
        noticeList.clear()
        for (id in noticeIds) {
            val title = sharedPref.getString("notice_title_$id", "")
            val content = sharedPref.getString("notice_content_$id", "")
            val date = sharedPref.getString("notice_date_$id", "")
            val targetSem = sharedPref.getString("notice_sem_$id", "All") ?: "All"
            val subject = sharedPref.getString("notice_subject_$id", "General") ?: "General"
            
            if (title != null && content != null && date != null) {
                noticeList.add(Notice(id, title, content, date, targetSem, subject))
            }
        }
        noticeList.sortByDescending { it.date }
        adapter = NoticeAdapter(noticeList) { notice -> deleteNotice(notice) }
        recyclerView.adapter = adapter
    }

    private fun showAddNoticeDialog() {
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val role = userPref.getString("role_${userPref.getString("current_userId", "")}", "")
        val currentFacultyId = userPref.getString("current_userId", "") ?: ""

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Notice")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        // For Faculty: Subject Selection
        val facultyAllocations = mutableListOf<Pair<String, String>>()
        if (role == "Faculty") {
            val allocPref = getSharedPreferences("AllocationData", Context.MODE_PRIVATE)
            val allAllocIds = allocPref.getStringSet("allocation_ids", setOf()) ?: setOf()
            for (allocId in allAllocIds) {
                if (allocPref.getString("fac_id_$allocId", "") == currentFacultyId) {
                    val sub = allocPref.getString("subject_$allocId", "") ?: ""
                    val sem = allocPref.getString("semester_$allocId", "") ?: ""
                    facultyAllocations.add(sub to sem)
                }
            }
        }

        val selectionSpinner = Spinner(this)
        if (role == "Faculty") {
            if (facultyAllocations.isEmpty()) {
                Toast.makeText(this, "No subjects allocated", Toast.LENGTH_SHORT).show()
                return
            }
            val displayList = facultyAllocations.map { "${it.first} (${it.second})" }
            selectionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, displayList)
        } else {
            val sems = arrayOf("All", "Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6", "Sem 7", "Sem 8")
            selectionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sems)
        }

        val titleEt = EditText(this)
        titleEt.hint = "Notice Title"
        
        val contentEt = EditText(this)
        contentEt.hint = "Notice Description"
        contentEt.minLines = 3

        layout.addView(TextView(this).apply { text = if(role == "Faculty") "Select Subject:" else "Target Semester:"; setPadding(0, 10, 0, 5) })
        layout.addView(selectionSpinner)
        layout.addView(titleEt)
        layout.addView(contentEt)

        builder.setView(layout)

        builder.setPositiveButton("Post") { _, _ ->
            val title = titleEt.text.toString().trim()
            val content = contentEt.text.toString().trim()
            
            val targetSem: String
            val subject: String
            
            if (role == "Faculty") {
                val selected = facultyAllocations[selectionSpinner.selectedItemPosition]
                subject = selected.first
                targetSem = selected.second
            } else {
                subject = "General"
                targetSem = selectionSpinner.selectedItem.toString()
            }
            
            if (title.isNotEmpty() && content.isNotEmpty()) {
                saveNotice(title, content, targetSem, subject)
                NotificationHelper.sendNotification(
                    this, 
                    "📣 New Notice ($targetSem - $subject): $title", 
                    content, 
                    NotificationsActivity::class.java
                )
            } else {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun saveNotice(title: String, content: String, targetSem: String, subject: String) {
        val sharedPref = getSharedPreferences("CampusData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        
        val noticeId = UUID.randomUUID().toString()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        editor.putString("notice_title_$noticeId", title)
        editor.putString("notice_content_$noticeId", content)
        editor.putString("notice_date_$noticeId", date)
        editor.putString("notice_sem_$noticeId", targetSem)
        editor.putString("notice_subject_$noticeId", subject)

        val noticeIds = sharedPref.getStringSet("notice_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = noticeIds.toMutableSet()
        updatedIds.add(noticeId)
        editor.putStringSet("notice_ids", updatedIds)
        
        editor.apply()
        Toast.makeText(this, "Notice Posted for $subject ($targetSem)", Toast.LENGTH_SHORT).show()
        loadNotices()
    }

    private fun deleteNotice(notice: Notice) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notice")
            .setMessage("Are you sure you want to delete this notice?")
            .setPositiveButton("Delete") { _, _ ->
                val sharedPref = getSharedPreferences("CampusData", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                
                editor.remove("notice_title_${notice.id}")
                editor.remove("notice_content_${notice.id}")
                editor.remove("notice_date_${notice.id}")
                editor.remove("notice_sem_${notice.id}")
                editor.remove("notice_subject_${notice.id}")

                val noticeIds = sharedPref.getStringSet("notice_ids", mutableSetOf()) ?: mutableSetOf()
                val updatedIds = noticeIds.toMutableSet()
                updatedIds.remove(notice.id)
                editor.putStringSet("notice_ids", updatedIds)
                
                editor.apply()
                loadNotices()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    data class Notice(val id: String, val title: String, val content: String, val date: String, val targetSem: String, val subject: String)

    class NoticeAdapter(private val list: List<Notice>, val onDelete: (Notice) -> Unit) : RecyclerView.Adapter<NoticeAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleTv: TextView = view.findViewById(R.id.noticeTitleTv)
            val contentTv: TextView = view.findViewById(R.id.noticeContentTv)
            val dateTv: TextView = view.findViewById(R.id.noticeDateTv)
            val deleteBtn: ImageButton = view.findViewById(R.id.deleteNoticeBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notice, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val item = list[position]
            h.titleTv.text = "[${item.subject} - ${item.targetSem}] ${item.title}"
            h.contentTv.text = item.content
            h.dateTv.text = "Posted on: ${item.date}"
            h.deleteBtn.setOnClickListener { onDelete(item) }
        }
        override fun getItemCount() = list.size
    }
}
