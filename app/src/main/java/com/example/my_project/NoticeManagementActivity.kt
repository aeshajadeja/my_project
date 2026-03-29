package com.example.my_project

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
            if (title != null && content != null && date != null) {
                noticeList.add(Notice(id, title, content, date))
            }
        }
        noticeList.sortByDescending { it.date }
        adapter = NoticeAdapter(noticeList) { notice -> deleteNotice(notice) }
        recyclerView.adapter = adapter
    }

    private fun showAddNoticeDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add New Notice")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val titleEt = EditText(this)
        titleEt.hint = "Notice Title"
        layout.addView(titleEt)

        val contentEt = EditText(this)
        contentEt.hint = "Notice Description"
        contentEt.minLines = 3
        layout.addView(contentEt)

        builder.setView(layout)

        builder.setPositiveButton("Post") { _, _ ->
            val title = titleEt.text.toString().trim()
            val content = contentEt.text.toString().trim()
            if (title.isNotEmpty() && content.isNotEmpty()) {
                saveNotice(title, content)
                // Trigger Push Notification
                NotificationHelper.sendNotification(
                    this, 
                    "📣 New Notice: $title", 
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

    private fun saveNotice(title: String, content: String) {
        val sharedPref = getSharedPreferences("CampusData", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        
        val noticeId = UUID.randomUUID().toString()
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        editor.putString("notice_title_$noticeId", title)
        editor.putString("notice_content_$noticeId", content)
        editor.putString("notice_date_$noticeId", date)

        val noticeIds = sharedPref.getStringSet("notice_ids", mutableSetOf()) ?: mutableSetOf()
        val updatedIds = noticeIds.toMutableSet()
        updatedIds.add(noticeId)
        editor.putStringSet("notice_ids", updatedIds)
        
        editor.apply()
        Toast.makeText(this, "Notice Posted", Toast.LENGTH_SHORT).show()
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

    data class Notice(val id: String, val title: String, val content: String, val date: String)

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
            h.titleTv.text = item.title
            h.contentTv.text = item.content
            h.dateTv.text = "Posted on: ${item.date}"
            h.deleteBtn.setOnClickListener { onDelete(item) }
        }
        override fun getItemCount() = list.size
    }
}
