package com.example.my_project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatUserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTv: TextView
    private lateinit var searchEt: EditText
    private lateinit var adapter: UserAdapter
    private val allUsers = mutableListOf<User>()
    private val filteredList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_user_list)

        recyclerView = findViewById(R.id.usersRecyclerView)
        emptyTv = findViewById(R.id.emptyTv)
        searchEt = findViewById(R.id.searchUserEt)
        
        recyclerView.layoutManager = LinearLayoutManager(this)

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }

        loadUsers()

        searchEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterUsers(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadUsers() {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentUserId = sharedPref.getString("current_userId", "") ?: ""
        val currentUserRole = sharedPref.getString("role_$currentUserId", "")
        val allIds = sharedPref.getStringSet("all_user_ids", setOf()) ?: setOf()
        
        allUsers.clear()
        for (id in allIds) {
            if (id == currentUserId) continue

            val role = sharedPref.getString("role_$id", "")
            val name = sharedPref.getString("name_$id", "Unknown")
            
            // Logic: 
            // - Students see ALL Faculty and ALL other Students
            // - Faculty see ALL Students
            // - Admins see Everyone
            val canChat = when (currentUserRole) {
                "Student" -> role == "Faculty" || role == "Student"
                "Faculty" -> role == "Student"
                "Admin" -> true
                else -> false
            }

            if (canChat) {
                allUsers.add(User(id, name!!, role!!))
            }
        }
        
        filteredList.clear()
        filteredList.addAll(allUsers)
        
        adapter = UserAdapter(filteredList) { user ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("RECEIVER_ID", user.id)
            intent.putExtra("RECEIVER_NAME", user.name)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        
        updateEmptyView()
    }

    private fun filterUsers(query: String) {
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(allUsers)
        } else {
            val lowerCaseQuery = query.lowercase()
            for (user in allUsers) {
                if (user.id.lowercase().contains(lowerCaseQuery) || 
                    user.name.lowercase().contains(lowerCaseQuery)) {
                    filteredList.add(user)
                }
            }
        }
        adapter.notifyDataSetChanged()
        updateEmptyView()
    }

    private fun updateEmptyView() {
        if (filteredList.isEmpty()) {
            emptyTv.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyTv.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    data class User(val id: String, val name: String, val role: String)

    class UserAdapter(private val list: List<User>, val onClick: (User) -> Unit) : 
        RecyclerView.Adapter<UserAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTv: TextView = view.findViewById(R.id.userNameTv)
            val idTv: TextView = view.findViewById(R.id.userIdTv)
            val roleTv: TextView = view.findViewById(R.id.userRoleTv)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user_chat, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val user = list[position]
            h.nameTv.text = user.name
            h.idTv.text = "ID: ${user.id}"
            h.roleTv.text = user.role
            h.itemView.setOnClickListener { onClick(user) }
        }

        override fun getItemCount() = list.size
    }
}
