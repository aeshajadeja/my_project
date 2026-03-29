package com.example.my_project

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserAdapter
    private var userList = mutableListOf<User>()
    private var roleFilter = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        roleFilter = intent.getStringExtra("ROLE_FILTER") ?: ""
        findViewById<TextView>(R.id.listTitleTv).text = "Manage $roleFilter"

        recyclerView = findViewById(R.id.userRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadUsers()

        findViewById<ImageButton>(R.id.backBtn).setOnClickListener { finish() }

        val searchEt = findViewById<EditText>(R.id.searchEt)
        searchEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filter(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadUsers() {
        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val allIds = sharedPref.getStringSet("all_user_ids", setOf()) ?: setOf()
        
        userList.clear()
        for (id in allIds) {
            val role = sharedPref.getString("role_$id", "")
            if (role == roleFilter) {
                val name = sharedPref.getString("name_$id", "Unknown")
                userList.add(User(id, name!!, role!!))
            }
        }
        adapter = UserAdapter(userList, { user -> editUser(user) }, { user -> deleteUser(user) })
        recyclerView.adapter = adapter
    }

    private fun filter(text: String) {
        val filteredList = userList.filter { 
            it.name.contains(text, ignoreCase = true) || it.id.contains(text, ignoreCase = true)
        }
        adapter.updateList(filteredList)
    }

    private fun editUser(user: User) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit ${user.role}")
        builder.setMessage("Enter new name for ID: ${user.id}")

        val inputName = EditText(this)
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 0, 50, 0)
        inputName.layoutParams = params
        inputName.setText(user.name)
        container.addView(inputName)
        builder.setView(container)

        builder.setPositiveButton("Update") { _, _ ->
            val newName = inputName.text.toString().trim()
            if (newName.isNotEmpty()) {
                val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                
                // 1. Remove old name from unique set
                val allNames = sharedPref.getStringSet("registered_names", mutableSetOf())?.toMutableSet()
                allNames?.remove(user.name.lowercase())
                
                // 2. Check if new name is already taken
                if (allNames?.contains(newName.lowercase()) == true) {
                    Toast.makeText(this, "This name is already registered", Toast.LENGTH_SHORT).show()
                } else {
                    // 3. Update data
                    editor.putString("name_${user.id}", newName)
                    allNames?.add(newName.lowercase())
                    editor.putStringSet("registered_names", allNames)
                    editor.apply()
                    
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()
                    loadUsers() // Refresh
                }
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun deleteUser(user: User) {
        AlertDialog.Builder(this)
            .setTitle("Delete User")
            .setMessage("Are you sure you want to remove ${user.name}?")
            .setPositiveButton("Delete") { _, _ ->
                val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.remove("name_${user.id}")
                editor.remove("password_${user.id}")
                editor.remove("role_${user.id}")
                val allIds = sharedPref.getStringSet("all_user_ids", mutableSetOf())?.toMutableSet()
                allIds?.remove(user.id)
                editor.putStringSet("all_user_ids", allIds)
                val allNames = sharedPref.getStringSet("registered_names", mutableSetOf())?.toMutableSet()
                allNames?.remove(user.name.lowercase())
                editor.putStringSet("registered_names", allNames)
                editor.apply()
                Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    data class User(val id: String, val name: String, val role: String)

    class UserAdapter(private var list: List<User>, val onEdit: (User) -> Unit, val onDelete: (User) -> Unit) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameTv: TextView = view.findViewById(R.id.userNameTv)
            val idTv: TextView = view.findViewById(R.id.userIdTv)
            val editBtn: ImageButton = view.findViewById(R.id.editUserBtn)
            val deleteBtn: ImageButton = view.findViewById(R.id.deleteUserBtn)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
            return ViewHolder(v)
        }
        override fun onBindViewHolder(h: ViewHolder, position: Int) {
            val user = list[position]
            h.nameTv.text = user.name
            h.idTv.text = "ID: ${user.id}"
            h.editBtn.setOnClickListener { onEdit(user) }
            h.deleteBtn.setOnClickListener { onDelete(user) }
        }
        override fun getItemCount() = list.size
        fun updateList(newList: List<User>) { list = newList; notifyDataSetChanged() }
    }
}
