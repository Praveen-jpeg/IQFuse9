package com.example.iqfuse8

import android.util.Log

object LoggingManager {
    private const val TAG = "IQFuse"

    fun logStreakUpdate(userId: String, newStreak: Int, reason: String) {
        Log.d(TAG, "Streak Update - User: $userId, New: $newStreak, Reason: $reason")
    }

    fun logStreakUpdate(userId: String, oldStreak: Int, newStreak: Int, reason: String) {
        Log.d(TAG, "Streak Update - User: $userId, Old: $oldStreak, New: $newStreak, Reason: $reason")
    }

    fun logBadgeAwarded(userId: String, badgeType: BadgeType) {
        Log.d(TAG, "Badge Awarded - User: $userId, Badge: ${badgeType.displayName}")
    }

    fun logDailyChallenge(userId: String, correct: Boolean, streak: Int) {
        Log.d(TAG, "Daily Challenge - User: $userId, Correct: $correct, Current Streak: $streak")
    }

    fun logError(operation: String, error: Exception) {
        Log.e(TAG, "Error in $operation: ${error.message}", error)
    }

    fun logUserStats(userId: String, stats: UserStats) {
        Log.d(TAG, """
            User Stats Update - User: $userId
            Total Completed: ${stats.totalChallengesCompleted}
            Current Streak: ${stats.streak}
            Correct in Row: ${stats.correctAnswersInRow}
        """.trimIndent())
    }
} 