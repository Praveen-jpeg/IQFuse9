package com.example.iqfuse8

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.iqfuse8.adapter.BadgeAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BadgeGridActivity : AppCompatActivity() {

    private lateinit var badgeRecyclerView: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_badge_grid)

        badgeRecyclerView = findViewById(R.id.badgeRecyclerView)
        badgeRecyclerView.layoutManager = GridLayoutManager(this, 2)

        loadUserBadges()
    }

    private fun loadUserBadges() {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    val earnedBadgeKeys = document.get("badges") as? List<String> ?: emptyList()

        val allBadges = BadgeType.values().map { badgeType ->
            Badge(type = badgeType, earned = earnedBadgeKeys.contains(badgeType.key))
        }

        val adapter = BadgeAdapter(allBadges)
        badgeRecyclerView.adapter = adapter
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to load badges", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
