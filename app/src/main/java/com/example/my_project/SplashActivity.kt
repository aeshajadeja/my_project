package com.example.my_project

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)

        Log.d("SplashActivity", "Splash Screen Started")

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("SplashActivity", "Navigating to RegistrationActivity")
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
