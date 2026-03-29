package com.example.my_project

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.random.Random

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        checkNotificationPermission()

        val userIdEt = findViewById<EditText>(R.id.userIdEditText)
        val passwordEt = findViewById<EditText>(R.id.passwordEditText)
        val loginBtn = findViewById<Button>(R.id.loginButton)
        val forgotPasswordTv = findViewById<TextView>(R.id.forgotPasswordTv)
        val registerTv = findViewById<TextView>(R.id.registerTv)

        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)

        loginBtn.setOnClickListener {
            val userId = userIdEt.text.toString().trim()
            val password = passwordEt.text.toString().trim()
            val savedPassword = sharedPref.getString("password_$userId", "")

            if (userId.isNotEmpty() && password == savedPassword && savedPassword.isNotEmpty()) {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                val editor = sharedPref.edit()
                editor.putString("current_userId", userId)
                editor.apply()
                
                when {
                    userId.startsWith("ADM", ignoreCase = true) -> startActivity(Intent(this, AdminActivity::class.java))
                    userId.startsWith("FAC", ignoreCase = true) -> startActivity(Intent(this, FacultyActivity::class.java))
                    else -> startActivity(Intent(this, HomeActivity::class.java))
                }
                finish()
            } else {
                Toast.makeText(this, "Invalid ID or Password", Toast.LENGTH_SHORT).show()
            }
        }

        forgotPasswordTv.setOnClickListener { showVerificationDialog() }
        registerTv.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun showVerificationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Password")
        builder.setMessage("Enter your User ID to receive an OTP")
        val inputId = EditText(this)
        builder.setView(inputId)
        builder.setPositiveButton("Send OTP") { _, _ ->
            val userId = inputId.text.toString().trim()
            val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
            val savedPassword = sharedPref.getString("password_$userId", "")
            
            if (userId.isNotEmpty() && savedPassword!!.isNotEmpty()) {
                val generatedOtp = Random.nextInt(1000, 9999).toString()
                // Show OTP in a notification (Simulating SMS/Email)
                NotificationHelper.sendNotification(this, "🔒 OTP Recovery", "Your OTP for password reset is: $generatedOtp")
                showOtpVerificationDialog(userId, generatedOtp)
            } else {
                Toast.makeText(this, "ID not registered", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showOtpVerificationDialog(userId: String, correctOtp: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verify OTP")
        builder.setMessage("Enter the 4-digit OTP sent to your notifications")
        val inputOtp = EditText(this)
        builder.setView(inputOtp)
        builder.setPositiveButton("Verify") { _, _ ->
            if (inputOtp.text.toString() == correctOtp) {
                showResetPasswordDialog(userId)
            } else {
                Toast.makeText(this, "Incorrect OTP", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showResetPasswordDialog(userId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Reset Password")
        builder.setMessage("Enter your new password")
        val inputPass = EditText(this)
        builder.setView(inputPass)
        builder.setPositiveButton("Reset") { _, _ ->
            val newPassword = inputPass.text.toString().trim()
            if (newPassword.isNotEmpty()) {
                getSharedPreferences("UserData", Context.MODE_PRIVATE).edit().putString("password_$userId", newPassword).apply()
                Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }
}
