package com.example.iqfuse8

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var btnSolveAptitude: Button
    private lateinit var btnDailyChallenge: Button
    private lateinit var btnPlayTango: Button

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
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        requestNotificationPermission()
        initFirebase()
        initUI()
        loadUserName()
        scheduleDailyNotification()
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

        btnSolveAptitude = findViewById(R.id.btn_solve_aptitude)
        btnDailyChallenge = findViewById(R.id.btn_daily_challenge)
        btnPlayTango = findViewById(R.id.btn_play_tango)

        findViewById<ImageView>(R.id.hamburgerIcon).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        tvEditName.setOnClickListener { showEditNamePopup() }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> startActivity(Intent(this, DashboardActivity::class.java))
                R.id.nav_logout -> performLogout()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        btnSolveAptitude.setOnClickListener {
            startActivity(Intent(this, AptitudeTopicsActivity::class.java))
        }

        btnDailyChallenge.setOnClickListener {
            startActivity(Intent(this, DailyChallengeActivity::class.java))
        }

        btnPlayTango.setOnClickListener {
            startActivity(Intent(this, TangoGameActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun scheduleDailyNotification() {
        val dailyRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(15, TimeUnit.SECONDS) // Change this delay for testing
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
                    document.getString("username")?.let { tvGreeting.text = "Hi $it" }
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
