package com.example.iqfuse8

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tvGreeting: TextView
    private lateinit var tvEditName: TextView
    private lateinit var tvStreak: TextView

    // Aptitude section buttons
    private lateinit var btnSolveAptitude: Button
    private lateinit var btnDailyChallenge: Button
    private lateinit var btnPlayTango: Button

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var streakReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        // Initialize Firestore and enable offline caching
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Enables local caching
            .build()
        firestore.firestoreSettings = settings

        auth = FirebaseAuth.getInstance()

        // Initialize DrawerLayout from activity_main.xml
        drawerLayout = findViewById(R.id.drawer_layout)

        // Adjust main content for system windows
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the hamburger icon to open the navigation drawer
        val hamburgerIcon = findViewById<ImageView>(R.id.hamburgerIcon)
        hamburgerIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Get references to NavigationView header views from nav_header.xml
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        tvGreeting = headerView.findViewById(R.id.tvGreeting)
        tvEditName = headerView.findViewById(R.id.tvEditName)
        tvStreak = headerView.findViewById(R.id.tvStreak) // ✅ Make sure this is initialized

        // Set click listener for "Edit/Create Name" in the drawer header
        tvEditName.setOnClickListener {
            showEditNamePopup()
        }

        // Load stored username & streak from Firestore
        loadUserName()
        loadUserStreak()

        // Set up listener for navigation menu items (e.g., Logout)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    performLogout()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
        }

        // Set up BroadcastReceiver to update streak dynamically
        streakReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val newStreak = intent?.getIntExtra("newStreak", 0) ?: 0
                tvStreak.text = "Streak: $newStreak"
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(streakReceiver, IntentFilter("UPDATE_STREAK"))

        // Set up the Aptitude section buttons (in the main content)
        btnSolveAptitude = findViewById(R.id.btn_solve_aptitude)
        btnDailyChallenge = findViewById(R.id.btn_daily_challenge)
        btnPlayTango = findViewById(R.id.btn_play_tango)

        btnSolveAptitude.setOnClickListener {
            startActivity(Intent(this, AptitudeTopicsActivity::class.java))
        }
        btnDailyChallenge.setOnClickListener {
            startActivity(Intent(this, DailyChallengeActivity::class.java))
        }
        btnPlayTango.setOnClickListener {
            startActivity(Intent(this, TangoGameActivity::class.java))
        }
    }

    private fun showEditNamePopup() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter your name")

        // Create an EditText for user input
        val input = EditText(this)
        input.hint = "Your name"
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val name = input.text.toString().trim()
            if (name.isNotEmpty()) {
                tvGreeting.text = "Hi $name"
                saveUserNameToFirestore(name)
                Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.create().show()
    }

    private fun saveUserNameToFirestore(name: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userRef = firestore.collection("users").document(userId)
            val userData = hashMapOf("username" to name)

            // Merge with existing data (allows adding fields later)
            userRef.set(userData, SetOptions.merge())
                .addOnSuccessListener {
                    tvGreeting.text = "Hi $name" // ✅ Update UI in navigation drawer
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update name: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserName() {
        val userId = auth.currentUser?.uid
        userId?.let {
            firestore.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    document.getString("username")?.let { name ->
                        tvGreeting.text = "Hi $name"
                    }
                }
        }
    }

    private fun loadUserStreak() {
        val userId = auth.currentUser?.uid
        userId?.let {
            firestore.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    val streak = document.getLong("streak")?.toInt() ?: 0
                    tvStreak.text = "Streak: $streak" // ✅ Update streak in menu
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load streak", Toast.LENGTH_SHORT).show()
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
