package com.example.my_project

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val nameEt = findViewById<EditText>(R.id.nameEditText)
        val userIdEt = findViewById<EditText>(R.id.userIdEditText)
        val passwordEt = findViewById<EditText>(R.id.passwordEditText)
        val registerBtn = findViewById<Button>(R.id.registerButton)
        val loginTv = findViewById<TextView>(R.id.loginTextView)

        registerBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val userId = userIdEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()

            val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)

            if (name.isEmpty() || userId.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.equals(password, ignoreCase = true)) {
                Toast.makeText(this, "Name and Password cannot be the same", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (sharedPref.contains("password_$userId")) {
                Toast.makeText(this, "This User ID is already registered", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val allRegisteredNames = sharedPref.getStringSet("registered_names", mutableSetOf()) ?: mutableSetOf()
            if (allRegisteredNames.contains(name.lowercase())) {
                Toast.makeText(this, "This name is already registered in the system", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Automatic Role Detection based on ID format
            val role = when {
                userId.startsWith("ADM", ignoreCase = true) -> "Admin"
                userId.startsWith("FAC", ignoreCase = true) -> "Faculty"
                userId.length == 10 && userId.all { it.isDigit() } -> "Student"
                else -> null
            }

            if (role != null) {
                val editor = sharedPref.edit()
                
                // Save user data
                editor.putString("name_$userId", name)
                editor.putString("password_$userId", password)
                editor.putString("role_$userId", role)
                editor.putBoolean("active_$userId", true)
                
                // Track global lists for Admin Management
                val updatedNames = allRegisteredNames.toMutableSet()
                updatedNames.add(name.lowercase())
                editor.putStringSet("registered_names", updatedNames)

                val allUserIds = sharedPref.getStringSet("all_user_ids", mutableSetOf()) ?: mutableSetOf()
                val updatedIds = allUserIds.toMutableSet()
                updatedIds.add(userId)
                editor.putStringSet("all_user_ids", updatedIds)
                
                editor.apply()

                Toast.makeText(this, "Registered successfully as $role", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Invalid ID format. Use 10-digits for Student, FAC-ID for Faculty, or ADM-ID for Admin", Toast.LENGTH_LONG).show()
            }
        }

        loginTv.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
