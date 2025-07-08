package com.example.iqfuse8

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeaderboardAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyLeaderboard: TextView
    private val TAG = "LeaderboardActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        // Initialize RecyclerView and other views
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyLeaderboard = findViewById(R.id.tvEmptyLeaderboard)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = LeaderboardAdapter()
        recyclerView.adapter = adapter

        // Load leaderboard data
        loadLeaderboardData()
    }

    private fun loadLeaderboardData() {
        // Show loading indicator
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvEmptyLeaderboard.visibility = View.GONE

        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val leaderboardDocId = "leaderboard_$dayOfMonth"

        FirebaseFirestore.getInstance().collection("tango_leaderboards")
            .document(leaderboardDocId)
            .collection("entries")
            .orderBy("completionTime")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    progressBar.visibility = View.GONE
                    tvEmptyLeaderboard.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                
                val leaderboardEntries = mutableListOf<LeaderboardAdapter.LeaderboardEntry>()
                var completedFetches = 0
                
                // For each leaderboard entry, fetch the user's name from the users collection
                for (doc in documents) {
                    val userId = doc.id
                    val completionTime = doc.getLong("completionTime")?.toInt() ?: 0
                    val timestamp = doc.getLong("timestamp") ?: 0
                    
                    // Fetch username from users collection
                    FirebaseFirestore.getInstance().collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            // Get the username from the user's document
                            val username = userDoc.getString("username") ?: "Anonymous Player"
                            
                            // Create leaderboard entry with username
                            val entry = LeaderboardAdapter.LeaderboardEntry(
                                userId = userId,
                                displayName = username,
                                completionTime = completionTime,
                                timestamp = timestamp
                            )
                            
                            // Add to our list
                            leaderboardEntries.add(entry)
                            completedFetches++
                            
                            // When all fetches are complete, update UI
                            if (completedFetches == documents.size()) {
                                // Sort by completion time
                                leaderboardEntries.sortBy { it.completionTime }
                                
                                // Hide progress bar and update adapter
                                progressBar.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                                adapter.updateEntries(leaderboardEntries)
                                
                                Log.d(TAG, "Leaderboard loaded with ${leaderboardEntries.size} entries")
                            }
                        }
                        .addOnFailureListener { e ->
                            // If we can't get the user's name, use what's in the leaderboard entry
                            val displayName = doc.getString("displayName") ?: "Anonymous Player"
                            
                            val entry = LeaderboardAdapter.LeaderboardEntry(
                                userId = userId,
                                displayName = displayName,
                                completionTime = completionTime,
                                timestamp = timestamp
                            )
                            
                            leaderboardEntries.add(entry)
                            completedFetches++
                            
                            // When all fetches are complete, update UI
                            if (completedFetches == documents.size()) {
                                // Sort by completion time
                                leaderboardEntries.sortBy { it.completionTime }
                                
                                // Hide progress bar and update adapter
                                progressBar.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                                adapter.updateEntries(leaderboardEntries)
                                
                                Log.d(TAG, "Leaderboard loaded with ${leaderboardEntries.size} entries")
                            }
                            
                            Log.e(TAG, "Error fetching user data for $userId: ${e.message}", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                tvEmptyLeaderboard.visibility = View.VISIBLE
                Log.e(TAG, "Error loading leaderboard: ${e.message}", e)
                Toast.makeText(this, "Error loading leaderboard", Toast.LENGTH_SHORT).show()
            }
    }
} 