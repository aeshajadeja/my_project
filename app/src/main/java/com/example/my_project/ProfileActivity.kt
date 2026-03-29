package com.example.my_project

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var nameEt: TextInputEditText
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val backBtn = findViewById<ImageButton>(R.id.backBtn)
        profileImageView = findViewById(R.id.profileImageView)
        val changePicCard = findViewById<MaterialCardView>(R.id.changeProfilePicCard)
        nameEt = findViewById(R.id.editProfileName)
        val idTv = findViewById<TextView>(R.id.profileIdTv)
        val saveBtn = findViewById<Button>(R.id.saveProfileBtn)
        
        val studentActionsLayout = findViewById<LinearLayout>(R.id.studentActionsLayout)
        val attendanceCard = findViewById<MaterialCardView>(R.id.profileAttendanceCard)
        val timetableCard = findViewById<MaterialCardView>(R.id.profileTimetableCard)

        val sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentId = sharedPref.getString("current_userId", "") ?: ""
        val currentName = sharedPref.getString("name_$currentId", "")
        val currentImgUri = sharedPref.getString("image_$currentId", null)
        val role = sharedPref.getString("role_$currentId", "Student")

        // Load Current Data
        nameEt.setText(currentName)
        idTv.text = "User ID: $currentId"
        
        if (currentImgUri != null) {
            try {
                profileImageView.setImageURI(Uri.parse(currentImgUri))
                profileImageView.setPadding(0, 0, 0, 0)
            } catch (e: Exception) {
                profileImageView.setImageResource(android.R.drawable.ic_menu_myplaces)
            }
        }

        // Role-based UI visibility
        if (role == "Student") {
            studentActionsLayout.visibility = View.VISIBLE
        } else {
            studentActionsLayout.visibility = View.GONE
        }

        val getAction = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                selectedImageUri = result.data?.data
                selectedImageUri?.let { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } catch (e: Exception) {}
                    profileImageView.setImageURI(uri)
                    profileImageView.setPadding(0, 0, 0, 0)
                }
            }
        }

        changePicCard.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            getAction.launch(intent)
        }

        saveBtn.setOnClickListener {
            val newName = nameEt.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val editor = sharedPref.edit()
            editor.putString("name_$currentId", newName)
            if (selectedImageUri != null) {
                editor.putString("image_$currentId", selectedImageUri.toString())
            }
            editor.apply()

            Toast.makeText(this, "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
            finish()
        }

        backBtn.setOnClickListener { finish() }
        attendanceCard.setOnClickListener { startActivity(Intent(this, AttendanceActivity::class.java)) }
        timetableCard.setOnClickListener { startActivity(Intent(this, TimetableActivity::class.java)) }
    }
}
