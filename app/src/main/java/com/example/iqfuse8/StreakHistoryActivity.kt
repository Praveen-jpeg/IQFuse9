package com.example.iqfuse8

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.iqfuse8.adapter.StreakHistoryAdapter
import com.example.iqfuse8.model.StreakHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StreakHistoryActivity : AppCompatActivity() {
    private lateinit var rvStreakHistory: RecyclerView
    private lateinit var tvNoHistory: TextView
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streak_history)

        rvStreakHistory = findViewById(R.id.rvStreakHistory)
        tvNoHistory = findViewById(R.id.tvNoHistory)
        rvStreakHistory.layoutManager = LinearLayoutManager(this)

        loadStreakHistory()
    }

    private fun loadStreakHistory() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .collection("streakHistory")
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val history = documents.mapNotNull { doc ->
                    StreakHistory(
                        date = doc.getString("date") ?: "",
                        streak = doc.getLong("streak")?.toInt() ?: 0,
                        reason = doc.getString("reason") ?: ""
                    )
                }
                if (history.isEmpty()) {
                    tvNoHistory.visibility = View.VISIBLE
                    rvStreakHistory.visibility = View.GONE
                } else {
                    tvNoHistory.visibility = View.GONE
                    rvStreakHistory.visibility = View.VISIBLE
                    rvStreakHistory.adapter = StreakHistoryAdapter(history)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load streak history", Toast.LENGTH_SHORT).show()
            }
    }
} 