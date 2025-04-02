package com.example.iqfuse8

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit

class DailyChallengeActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var questionText: TextView
    private lateinit var optionsGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var resultText: TextView
    private lateinit var explanationText: TextView

    private var currentQuestion: String? = null
    private var correctAnswer: String? = null
    private var explanation: String? = null
    private var selectedAnswer: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_challenge)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("DailyChallengePrefs", Context.MODE_PRIVATE)

        // Bind UI elements
        questionText = findViewById(R.id.tvQuestion)
        optionsGroup = findViewById(R.id.rgOptions)
        submitButton = findViewById(R.id.btnSubmit)
        resultText = findViewById(R.id.tvResult)
        explanationText = findViewById(R.id.tvExplanation)

        loadDailyChallenge()
    }

    private fun loadDailyChallenge() {
        val lastAssignedTime = sharedPreferences.getLong("lastAssignedTime", 0)
        val currentTime = System.currentTimeMillis()

        if (TimeUnit.MILLISECONDS.toHours(currentTime - lastAssignedTime) < 24) {
            displayStoredQuestion()
        } else {
            assignNewChallenge()
        }
    }

    private fun assignNewChallenge() {
        firestore.collection("questions")
            .get()
            .addOnSuccessListener { topicDocuments ->
                if (topicDocuments.isEmpty) {
                    Log.e("FirestoreError", "No topics found")
                    return@addOnSuccessListener
                }

                val randomTopic = topicDocuments.documents.random()
                Log.d("FirestoreData", "Selected Topic: ${randomTopic.id}")

                randomTopic.reference.collection("sets")
                    .get()
                    .addOnSuccessListener { setDocuments ->
                        if (setDocuments.isEmpty) {
                            Log.e("FirestoreError", "No sets found in ${randomTopic.id}")
                            return@addOnSuccessListener
                        }

                        val randomSet = setDocuments.documents.random()
                        Log.d("FirestoreData", "Selected Set: ${randomSet.id}")

                        val questionFields = randomSet.data
                        if (questionFields.isNullOrEmpty()) {
                            Log.e("FirestoreError", "No questions in set ${randomSet.id}")
                            return@addOnSuccessListener
                        }

                        val randomQuestionKey = questionFields.keys.random()
                        val questionData = questionFields[randomQuestionKey] as? Map<String, Any>

                        if (questionData == null) {
                            Log.e("FirestoreError", "Invalid question format in ${randomSet.id}")
                            return@addOnSuccessListener
                        }

                        currentQuestion = questionData["question"] as? String
                        correctAnswer = questionData["answer"] as? String
                        explanation = questionData["explanation"] as? String
                        val options = questionData["options"] as? List<String>

                        if (currentQuestion != null && correctAnswer != null && options != null) {
                            saveDailyChallenge(currentQuestion!!, options, correctAnswer!!, explanation ?: "")
                            displayQuestion(currentQuestion!!, options)
                        } else {
                            Log.e("FirestoreError", "Missing fields in question data")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreError", "Error fetching sets: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error fetching topics: ${e.message}")
            }
    }

    private fun saveDailyChallenge(question: String, options: List<String>, answer: String, explanation: String) {
        sharedPreferences.edit().apply {
            putString("currentQuestion", question)
            putStringSet("options", options.toSet())
            putString("correctAnswer", answer)
            putString("explanation", explanation)
            putLong("lastAssignedTime", System.currentTimeMillis())
            putBoolean("answered", false)
            apply()
        }
    }

    private fun displayStoredQuestion() {
        currentQuestion = sharedPreferences.getString("currentQuestion", null)
        correctAnswer = sharedPreferences.getString("correctAnswer", null)
        explanation = sharedPreferences.getString("explanation", null)
        val options = sharedPreferences.getStringSet("options", emptySet())?.toList() ?: emptyList()

        if (currentQuestion.isNullOrEmpty() || options.isEmpty()) {
            Log.e("SharedPreferences", "No stored question found")
            assignNewChallenge()
        } else {
            displayQuestion(currentQuestion!!, options)
            if (sharedPreferences.getBoolean("answered", false)) {
                showAnswer()
            }
        }
    }

    private fun displayQuestion(question: String, options: List<String>) {
        questionText.text = question
        optionsGroup.removeAllViews()

        for (option in options) {
            val radioButton = RadioButton(this)
            radioButton.text = option
            optionsGroup.addView(radioButton)
        }

        submitButton.setOnClickListener {
            val selectedId = optionsGroup.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRadioButton = findViewById<RadioButton>(selectedId)
                selectedAnswer = selectedRadioButton.text.toString()
                checkAnswer(selectedAnswer!!)
            } else {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAnswer(selected: String) {
        val isCorrect = selected == correctAnswer
        if (isCorrect) {
            increaseStreak()
            resultText.text = "Correct!"
            resultText.setTextColor(getColor(R.color.green))
        } else {
            resultText.text = "Wrong!"
            resultText.setTextColor(getColor(R.color.blue))
        }
        showAnswer()
        sharedPreferences.edit().putBoolean("answered", true).apply()
    }

    private fun showAnswer() {
        explanationText.text = "Correct Answer: $correctAnswer\nExplanation: $explanation"
        explanationText.visibility = TextView.VISIBLE
        optionsGroup.setOnCheckedChangeListener(null)
        for (i in 0 until optionsGroup.childCount) {
            optionsGroup.getChildAt(i).isEnabled = false
        }
        submitButton.isEnabled = false
    }

    private fun increaseStreak() {
        val currentStreak = sharedPreferences.getInt("streak", 0)
        sharedPreferences.edit().putInt("streak", currentStreak + 1).apply()
        Toast.makeText(this, "Streak increased!", Toast.LENGTH_SHORT).show()
    }
}
