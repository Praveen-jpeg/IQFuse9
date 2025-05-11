package com.example.iqfuse8

import android.os.Bundle
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
    private lateinit var tvTangoGameStreak: TextView
    private lateinit var tvLeaderboard: TextView
    private lateinit var rvBadges: RecyclerView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Bind UI elements
        tvUserName = findViewById(R.id.tvUserName)
        tvDailyChallengeStreak = findViewById(R.id.tvDailyChallengeStreak)
        tvTangoGameStreak = findViewById(R.id.tvTangoGameStreak)
        tvLeaderboard = findViewById(R.id.tvLeaderboard)
        rvBadges = findViewById(R.id.rvBadges)

        // Set up RecyclerView for badges
        rvBadges.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Fetch and display user details
        loadUserDetails()

        // Set click listeners
        tvTangoGameStreak.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }

        tvLeaderboard.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserDetails() {
        val userId = auth.currentUser?.uid
        userId?.let {
            firestore.collection("users").document(it).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("username") ?: "User"
                        val dailyChallengeStreak = document.getLong("streak") ?: 0
                        val tangoGameStreak = document.getLong("tangoGameStreak") ?: 0

                        // Set user details to the UI
                        tvUserName.text = "Name: $name"
                        tvDailyChallengeStreak.text = "Daily Challenge Streak: $dailyChallengeStreak"
                        tvTangoGameStreak.text = "Tango Game Streak: $tangoGameStreak"

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
