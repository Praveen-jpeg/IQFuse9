package com.example.iqfuse8

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.iqfuse8.adapter.BadgeAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvUserName: TextView
    private lateinit var tvDailyChallengeStreak: TextView
    private lateinit var tvLeaderboard: TextView
    private lateinit var rvBadges: RecyclerView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Enable back button in action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Player Dashboard"

        // Bind UI elements
        tvUserName = findViewById(R.id.tvUserName)
        tvDailyChallengeStreak = findViewById(R.id.tvDailyChallengeStreak)
        tvLeaderboard = findViewById(R.id.tvLeaderboard)
        rvBadges = findViewById(R.id.rvBadges)

        // Set up RecyclerView for badges
        rvBadges.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Fetch and display user details
        loadUserDetails()

        // Set click listeners
        tvLeaderboard.setOnClickListener {
            startActivity(Intent(this, LeaderboardActivity::class.java))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when coming back to this screen
        loadUserDetails()
    }

    private fun loadUserDetails() {
        val userId = auth.currentUser?.uid
        userId?.let {
            firestore.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Get basic user info
                        val name = document.getString("username") ?: "Player"
                        
                        // Get streak info with fallbacks to 0
                        val dailyChallengeStreak = document.getLong("streak") ?: 0
                        
                        // Set user details to the UI
                        tvUserName.text = "Name: $name"
                        tvDailyChallengeStreak.text = "Daily Challenge Streak: $dailyChallengeStreak 🔥"

                        // Load badges for the user
                        loadUserBadges(it)
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun loadUserBadges(userId: String) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val earnedBadgeKeys = document.get("badges") as? List<String> ?: emptyList()
                
                val badges = BadgeType.values().map { badgeType ->
                    Badge(type = badgeType, earned = earnedBadgeKeys.contains(badgeType.key))
                }

                // Set up the BadgeAdapter with the list of badges
                val badgeAdapter = BadgeAdapter(badges)
                rvBadges.adapter = badgeAdapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load badges", Toast.LENGTH_SHORT).show()
            }
    }
}
