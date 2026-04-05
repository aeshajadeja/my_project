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

class NotificationsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoticeAdapter
    private var noticeList = mutableListOf<Notice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        backBtn?.setOnClickListener { finish() }

        recyclerView = findViewById(R.id.noticeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadNotices()
    }

    private fun loadNotices() {
        val campusPref = getSharedPreferences("CampusData", Context.MODE_PRIVATE)
        val userPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        
        val currentId = userPref.getString("current_userId", "") ?: ""
        val studentSem = userPref.getString("current_sem_$currentId", "All") ?: "All"
        
        val noticeIds = campusPref.getStringSet("notice_ids", setOf()) ?: setOf()
        
        noticeList.clear()
        for (id in noticeIds) {
            val title = campusPref.getString("notice_title_$id", "") ?: ""
            val content = campusPref.getString("notice_content_$id", "") ?: ""
            val date = campusPref.getString("notice_date_$id", "") ?: ""
            val targetSem = campusPref.getString("notice_sem_$id", "All") ?: "All"
            
            // Filter: Show if it's for everyone OR matches student's semester
            if (title.isNotEmpty() && content.isNotEmpty()) {
                if (targetSem == "All" || targetSem == studentSem) {
                    noticeList.add(Notice(id, title, content, date, targetSem))
                }
            }
        }
        noticeList.sortByDescending { it.date }
        
        adapter = NoticeAdapter(noticeList)
        recyclerView.adapter = adapter
    }

    data class Notice(val id: String, val title: String, val content: String, val date: String, val targetSem: String)

    class NoticeAdapter(private val list: List<Notice>) : RecyclerView.Adapter<NoticeAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleTv: TextView = view.findViewById(R.id.studentNoticeTitle)
            val contentTv: TextView = view.findViewById(R.id.studentNoticeContent)
            val dateTv: TextView = view.findViewById(R.id.studentNoticeDate)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_notice_student, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val item = list[position]
            h.titleTv.text = if (item.targetSem != "All") "[${item.targetSem}] ${item.title}" else item.title
            h.contentTv.text = item.content
            h.dateTv.text = item.date
        }
        override fun getItemCount() = list.size
    }
}
