package com.example.iqfuse8

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DailyChallengeActivity : AppCompatActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var questionText: TextView
    private lateinit var optionsGroup: RadioGroup
    private lateinit var submitButton: Button
    private lateinit var resultText: TextView
    private lateinit var explanationText: TextView
    private lateinit var encouragementText: TextView
    private lateinit var timerText: TextView
    private lateinit var answerIndicator: ImageView
    private lateinit var tvCurrentStreak: TextView

    private var correctAnswer: String? = null
    private var explanation: String? = null
    private var selectedAnswer: String? = null
    private var countdownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_challenge)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("DailyChallengePrefs", Context.MODE_PRIVATE)

        // Bind UI elements
        questionText = findViewById(R.id.tvQuestion)
        optionsGroup = findViewById(R.id.rgOptions)
        submitButton = findViewById(R.id.btnSubmit)
        resultText = findViewById(R.id.tvResult)
        explanationText = findViewById(R.id.tvExplanation)
        encouragementText = findViewById(R.id.tvEncouragement)
        timerText = findViewById(R.id.tvTimer)
        answerIndicator = findViewById(R.id.ivIndicator)
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak)

        validateStreakBeforeLoad()
        loadDailyChallenge()
        startCountdownTimer()
        loadCurrentStreak()
    }

    private fun loadDailyChallenge() {
        val userId = auth.currentUser?.uid ?: return
        val todayDate = getCurrentDate()

        val userChallengeRef = firestore.collection("users").document(userId)
            .collection("dailyChallenge").document(todayDate)

        userChallengeRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val questionId = document.getString("questionId")
                val answered = document.getBoolean("answered") ?: false
                val selectedOption = document.getString("selectedOption") ?: ""

                if (!questionId.isNullOrEmpty()) {
                    fetchQuestionById(questionId, answered, selectedOption)
                }

            } else {
                assignNewQuestion(userChallengeRef)
            }
        }.addOnFailureListener {
            Log.e("FirestoreError", "Failed to fetch daily challenge: ${it.message}")
        }
    }

    private fun assignNewQuestion(userChallengeRef: com.google.firebase.firestore.DocumentReference) {
        firestore.collection("DailyChallenge").get().addOnSuccessListener { snapshot ->
            val questions = snapshot.documents
            if (questions.isNotEmpty()) {
                val randomQuestion = questions.random()
                val questionId = randomQuestion.id

                val challengeData = mapOf(
                    "questionId" to questionId,
                    "answered" to false,
                    "selectedOption" to ""
                )

                userChallengeRef.set(challengeData).addOnSuccessListener {
                    fetchQuestionById(questionId, false, "")
                }
            }
        }.addOnFailureListener {
            Log.e("FirestoreError", "Error fetching questions: ${it.message}")
        }
    }

    private fun fetchQuestionById(questionId: String, answered: Boolean, selectedOption: String) {
        firestore.collection("DailyChallenge").document(questionId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val questionText = document.getString("question") ?: "No question available"
                    val options = document.get("options") as? List<String> ?: emptyList()
                    correctAnswer = document.getString("correct_option")
                    explanation = document.getString("explanation")

                    displayQuestion(questionText, options)

                    if (answered) {
                        showAnswer(selectedOption)
                    }
                }
            }.addOnFailureListener {
                Log.e("FirestoreError", "Error fetching question: ${it.message}")
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
                submitAnswer(selectedAnswer!!)
            } else {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitAnswer(selected: String) {
        val userId = auth.currentUser?.uid ?: return
        val todayDate = getCurrentDate()

        val userChallengeRef = firestore.collection("users").document(userId)
            .collection("dailyChallenge").document(todayDate)

        val updateData = mapOf(
            "answered" to true,
            "selectedOption" to selected
        )

        userChallengeRef.update(updateData).addOnSuccessListener {
            checkAnswer(selected)
        }.addOnFailureListener {
            Log.e("FirestoreError", "Error updating answer: ${it.message}")
        }
    }

    private fun checkAnswer(selected: String) {
        showAnswer(selected)
        val isAnswerCorrect = selected == correctAnswer
        
        if (isAnswerCorrect) {
            increaseStreak()
        } else {
            resetStreak()
        }

        // Update user stats
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            UserStatsManager.updateUserStats(userId, isAnswerCorrect)
            
            // Check for badges after updating stats
            checkForBadges()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showAnswer(selected: String) {
        val isCorrect = selected == correctAnswer

        resultText.text = if (isCorrect) "Correct!" else "Wrong!"
        resultText.setTextColor(ContextCompat.getColor(this, if (isCorrect) R.color.green else R.color.red))

        explanationText.visibility = View.VISIBLE
        explanationText.text = "Correct Answer: $correctAnswer\nExplanation: $explanation"

        encouragementText.visibility = View.VISIBLE
        val encouragementMessage = if (isCorrect) {
            "<font color='#008000'>Congratulations! Come back tomorrow for another interesting problem</font>"
        } else {
            "<font color='#800000'>Failure is the </font><font color='#008000'>stepping stone</font><font color='#800000'> to Success!! Come back Tomorrow...</font>"
        }
        encouragementText.text = Html.fromHtml(encouragementMessage, Html.FROM_HTML_MODE_LEGACY)

        answerIndicator.visibility = View.VISIBLE
        answerIndicator.setImageResource(if (isCorrect) R.drawable.ic_correct else R.drawable.ic_wrong)

        for (i in 0 until optionsGroup.childCount) {
            optionsGroup.getChildAt(i).isEnabled = false
        }

        submitButton.isEnabled = false
    }

    private fun startCountdownTimer() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val currentTime = System.currentTimeMillis()
        val nextResetTime = calendar.timeInMillis
        val remainingTime = nextResetTime - currentTime

        countdownTimer?.cancel()
        countdownTimer = object : CountDownTimer(remainingTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = TimeUnit.MILLISECONDS.toHours(millisUntilFinished)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                timerText.text = "Next Challenge in: %02d:%02d:%02d".format(hours, minutes, seconds)
            }

            override fun onFinish() {
                timerText.text = "New Challenge Available!"
            }
        }.start()
    }

    private fun loadCurrentStreak() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val currentStreak = document.getLong("streak")?.toInt() ?: 0
                tvCurrentStreak.text = "Current Streak: $currentStreak"
            }
    }

    private fun updateStreakDisplay(newStreak: Int) {
        tvCurrentStreak.text = "Current Streak: $newStreak"
    }

    private fun increaseStreak() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentStreak = snapshot.getLong("streak")?.toInt() ?: 0
            val newStreak = currentStreak + 1
            
            // Update streak
            transaction.update(userRef, "streak", newStreak)
            
            // Record streak history
            val historyRef = userRef.collection("streakHistory").document()
            transaction.set(historyRef, mapOf(
                "date" to getCurrentDate(),
                "streak" to newStreak,
                "reason" to "correct"
            ))
            
            // Check for milestone
            if (newStreak % 5 == 0) {
                val milestoneRef = userRef.collection("streakHistory").document()
                transaction.set(milestoneRef, mapOf(
                    "date" to getCurrentDate(),
                    "streak" to newStreak,
                    "reason" to "milestone"
                ))
                showMilestoneNotification(newStreak)
            }
            
            Pair(currentStreak, newStreak)
        }.addOnSuccessListener { (oldStreak, newStreak) ->
            updateStreakDisplay(newStreak)
            LoggingManager.logStreakUpdate(userId, oldStreak, newStreak, "Increased streak")
        }.addOnFailureListener { e ->
            Log.e("DailyChallenge", "Error updating streak", e)
        }
    }

    private fun resetStreak() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)
        
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentStreak = snapshot.getLong("streak")?.toInt() ?: 0
            
            // Record streak history before reset
            val historyRef = userRef.collection("streakHistory").document()
            transaction.set(historyRef, mapOf(
                "date" to getCurrentDate(),
                "streak" to currentStreak,
                "reason" to "wrong"
            ))
            
            // Reset streak
            transaction.update(userRef, "streak", 0)
            currentStreak
        }.addOnSuccessListener { oldStreak ->
            updateStreakDisplay(0)
            LoggingManager.logStreakUpdate(userId, oldStreak, 0, "Reset streak")
        }.addOnFailureListener { e ->
            Log.e("DailyChallenge", "Error resetting streak", e)
        }
    }

    private fun showMilestoneNotification(streak: Int) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "streak_milestones"
        
        // Create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Streak Milestones",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Streak Milestone!")
            .setContentText("Congratulations! You've reached a ${streak}-day streak!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(streak, notification)
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun validateStreakBeforeLoad() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            val lastPlayedDate = document.getString("lastPlayedDate")
            val today = getCurrentDate()
            val oldStreak = document.getLong("streak")?.toInt() ?: 0

            if (lastPlayedDate != null) {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val lastDate = format.parse(lastPlayedDate)
                val currentDate = format.parse(today)
                
                if (lastDate != null && currentDate != null) {
                    val diffInDays = TimeUnit.MILLISECONDS.toDays(currentDate.time - lastDate.time)
                    
                    // Only reset streak if more than one day has passed
                    if (diffInDays > 1) {
                        userRef.update(
                            mapOf(
                                "streak" to 0,
                                "lastPlayedDate" to today
                            )
                        ).addOnSuccessListener {
                            LoggingManager.logStreakUpdate(userId, oldStreak, 0, "Missed more than one day")
                            updateMainActivityStreak(0)
                        }
                    }
                }
            }
        }
    }

    private fun checkForBadges() {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val totalCompleted = doc.getLong("totalChallengesCompleted")?.toInt() ?: 0
                    val streak = doc.getLong("streak")?.toInt() ?: 0
                    val correctAnswersInRow = doc.getLong("correctAnswersInRow")?.toInt() ?: 0
                    val earnedBadges = doc.get("badges") as? List<String> ?: emptyList()

                    val userStats = UserStats(
                        totalChallengesCompleted = totalCompleted,
                        streak = streak,
                        correctAnswersInRow = correctAnswersInRow
                    )

                    BadgeManager.checkAndAwardBadges(userStats, earnedBadges.toSet()) { badge ->
                        // Award new badge
                        Toast.makeText(this, "🎉 New badge earned: ${badge.displayName}", Toast.LENGTH_LONG).show()

                        // Save to Firestore
                        val updatedBadges = earnedBadges.toMutableList().apply {
                            add(badge.key)
                        }
                        firestore.collection("users").document(userId)
                            .update("badges", updatedBadges)
                    }
                }
        }
    }

    private fun updateMainActivityStreak(newStreak: Int) {
        val intent = Intent("UPDATE_STREAK").putExtra("newStreak", newStreak)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}