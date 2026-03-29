package com.example.my_project

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class QuizActivity : AppCompatActivity() {

    private lateinit var questionNumberTv: TextView
    private lateinit var questionTv: TextView
    private lateinit var scoreTv: TextView
    private lateinit var optionButtons: List<MaterialButton>
    private lateinit var submitBtn: MaterialButton

    private var currentQuestionIndex = 0
    private var score = 0
    private var selectedOptionIndex = -1

    private val questions = listOf(
        Question("Which language is primarily used for Android Development?", listOf("Swift", "Kotlin", "C#", "Objective-C"), 1),
        Question("What does SDK stand for?", listOf("System Design Kit", "Software Development Kit", "Standard Data Key", "Software Design Kit"), 1),
        Question("Which layout is used to align views in a linear direction?", listOf("ConstraintLayout", "RelativeLayout", "LinearLayout", "FrameLayout"), 2),
        Question("Which file defines the UI of an Android screen?", listOf("Java file", "XML file", "Gradle file", "Manifest file"), 1),
        Question("What is the latest stable Android version name?", listOf("Upside Down Cake", "Vanilla Ice Cream", "Tiramisu", "S'mores"), 0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        // Initialize Views
        questionNumberTv = findViewById(R.id.questionNumberTv)
        questionTv = findViewById(R.id.questionTv)
        scoreTv = findViewById(R.id.scoreTv)
        submitBtn = findViewById(R.id.submitBtn)
        
        optionButtons = listOf(
            findViewById(R.id.option1Btn),
            findViewById(R.id.option2Btn),
            findViewById(R.id.option3Btn),
            findViewById(R.id.option4Btn)
        )

        findViewById<ImageButton>(R.id.backBtn)?.setOnClickListener { finish() }

        // Setup Option Clicks
        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                selectOption(index)
            }
        }

        submitBtn.setOnClickListener {
            handleNextButtonClick()
        }

        loadQuestion()
    }

    private fun loadQuestion() {
        if (currentQuestionIndex >= questions.size) {
            showFinalScore()
            return
        }

        val question = questions[currentQuestionIndex]
        questionNumberTv.text = "Question ${currentQuestionIndex + 1} of ${questions.size}"
        questionTv.text = question.text
        
        // Reset button styles properly for MaterialButtons
        optionButtons.forEachIndexed { index, button ->
            button.text = question.options[index]
            button.isEnabled = true
            button.strokeColor = android.content.res.ColorStateList.valueOf(Color.GRAY)
            button.setTextColor(Color.BLACK)
        }

        selectedOptionIndex = -1
        submitBtn.visibility = View.INVISIBLE
    }

    private fun selectOption(index: Int) {
        selectedOptionIndex = index
        
        // Reset all buttons to default outlined state
        optionButtons.forEach {
            it.strokeColor = android.content.res.ColorStateList.valueOf(Color.GRAY)
            it.setTextColor(Color.BLACK)
        }

        // Highlight the selected button
        val selectedButton = optionButtons[index]
        selectedButton.strokeColor = android.content.res.ColorStateList.valueOf(Color.BLUE)
        selectedButton.setTextColor(Color.BLUE)
        
        submitBtn.visibility = View.VISIBLE
    }

    private fun handleNextButtonClick() {
        val correctAnswerIndex = questions[currentQuestionIndex].correctIndex
        
        if (selectedOptionIndex == correctAnswerIndex) {
            score += 10
            scoreTv.text = "Score: $score"
            Toast.makeText(this, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Wrong! The answer was: ${questions[currentQuestionIndex].options[correctAnswerIndex]}", Toast.LENGTH_SHORT).show()
        }

        currentQuestionIndex++
        loadQuestion()
    }

    private fun showFinalScore() {
        AlertDialog.Builder(this)
            .setTitle("Quiz Completed!")
            .setMessage("Your final score is $score / ${questions.size * 10}")
            .setPositiveButton("Finish") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    data class Question(val text: String, val options: List<String>, val correctIndex: Int)
}
