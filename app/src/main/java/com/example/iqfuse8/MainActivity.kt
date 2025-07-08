package com.example.iqfuse8

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tvGreeting: TextView
    private lateinit var tvEditName: TextView
    private lateinit var tvStreak: TextView
    private lateinit var tvAppGreeting: TextView

    private lateinit var cardAptitude: CardView
    private lateinit var cardDailyChallenge: CardView
    private lateinit var cardTango: CardView

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val streakReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newStreak = intent?.getIntExtra("newStreak", 0) ?: 0
            tvStreak.text = "Streak: $newStreak"
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Initialize Firebase using the centralized manager
        FirebaseManager.initialize(applicationContext)
        setContentView(R.layout.activity_main)
        
        // Initialize Tango puzzles in the background to ensure availability
        FirebaseManager.initializeTangoPuzzles()

        requestNotificationPermission()
        initFirebase()
        initUI()
        loadUserName()
        scheduleDailyNotification()
        checkForBadges()

        // ✅ Handle notification intent
        val navigateTo = intent.getStringExtra("navigateTo")
        if (navigateTo == "dailyChallenge") {
            startActivity(Intent(this, DailyChallengeActivity::class.java))
        }
    }


    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST
            )
        }
    }

    private fun initFirebase() {
        firestore = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build()
        }
        auth = FirebaseAuth.getInstance()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(streakReceiver, IntentFilter("UPDATE_STREAK"))
    }

    private fun initUI() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)

        tvGreeting = headerView.findViewById(R.id.tvGreeting)
        tvEditName = headerView.findViewById(R.id.tvEditName)
        tvAppGreeting = findViewById(R.id.tvGreeting)

        // Initialize card views
        cardAptitude = findViewById(R.id.card_aptitude)
        cardDailyChallenge = findViewById(R.id.card_daily_challenge)
        cardTango = findViewById(R.id.card_tango)

        findViewById<ImageView>(R.id.hamburgerIcon).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        tvEditName.setOnClickListener { showEditNamePopup() }

        // Set up navigation menu
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Already in home
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_streak_history -> {
                    startActivity(Intent(this, StreakHistoryActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }

        // Set click listeners for cards
        cardAptitude.setOnClickListener {
            startActivity(Intent(this, AptitudeTopicsActivity::class.java))
        }

        cardDailyChallenge.setOnClickListener {
            startActivity(Intent(this, DailyChallengeActivity::class.java))
        }
        
        cardTango.setOnClickListener {
            startActivity(Intent(this, TangoGameActivityNew::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun scheduleDailyNotification() {
        val dailyRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_notification",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )
    }

    private fun showEditNamePopup() {
        val input = EditText(this).apply { hint = "Your name" }

        AlertDialog.Builder(this)
            .setTitle("Enter your name")
            .setView(input)
            .setPositiveButton("OK") { dialog, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    tvGreeting.text = "Hi $name"
                    saveUserNameToFirestore(name)
                } else {
                    Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun saveUserNameToFirestore(name: String) {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId)
                .set(mapOf("username" to name), SetOptions.merge())
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update name: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserName() {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val username = document.getString("username")
                    username?.let { 
                        tvGreeting.text = "Hi $it"
                        tvAppGreeting.text = "Welcome, $it!"
                    }
                }
        }
    }

    private fun checkForBadges() {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val totalCompleted = doc.getLong("totalChallengesCompleted")?.toInt() ?: 0
                    val currentStreak = doc.getLong("currentStreak")?.toInt() ?: 0
                    val correctAnswersInRow = doc.getLong("correctAnswersInRow")?.toInt() ?: 0
                    val earnedBadges = doc.get("badges") as? List<String> ?: emptyList()

                    val userStats = UserStats(
                        totalChallengesCompleted = totalCompleted,
                        streak = currentStreak,
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


    private fun performLogout() {
        auth.signOut()
        getSharedPreferences("app_prefs", MODE_PRIVATE).edit().clear().apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(streakReceiver)
    }
}