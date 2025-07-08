package com.example.iqfuse8.model

data class LeaderboardEntry(
    val userId: String = "",
    val username: String = "",
    val score: Int = 0,
    val streak: Int = 0,
    val lastUpdated: Long = 0
) {
    // Empty constructor for Firebase
    constructor() : this("", "", 0, 0, 0)
} 