package com.example.iqfuse8.model

data class StreakHistory(
    val date: String = "",
    val streak: Int = 0,
    val reason: String = "" // "correct", "wrong", "missed", "milestone"
) 