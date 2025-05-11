package com.example.iqfuse8

import android.util.Log

object BadgeManager {
    private data class BadgeCriteria(
        val type: BadgeType,
        val check: (UserStats) -> Boolean
    )

    private val badgeCriteriaList = listOf(
        BadgeCriteria(BadgeType.FIRST_WIN) { it.totalChallengesCompleted == 1 },
        BadgeCriteria(BadgeType.CENTURY_SOLVER) { it.totalChallengesCompleted >= 100 },
        BadgeCriteria(BadgeType.STREAK_ROOKIE) { it.streak >= 3 },
        BadgeCriteria(BadgeType.CONSISTENCY_STAR) { it.streak >= 7 },
        BadgeCriteria(BadgeType.DEDICATION_CHAMP) { it.streak >= 15 },
        BadgeCriteria(BadgeType.MASTER_STREAKER) { it.streak >= 30 },
        BadgeCriteria(BadgeType.UNBREAKABLE) { it.streak >= 100 },
        BadgeCriteria(BadgeType.SHARP_SHOOTER) { it.correctAnswersInRow >= 5 },
        BadgeCriteria(BadgeType.FLAWLESS_WEEK) { 
            it.streak == 7 && it.correctAnswersInRow == 7 
        }
    )

    fun checkAndAwardBadges(
        userStats: UserStats,
        badges: Set<String>, // Already earned badge keys
        onBadgeAwarded: (BadgeType) -> Unit
    ) {
        Log.d("BadgeDebug", "Starting badge check with stats: $userStats")
        Log.d("BadgeDebug", "Current badges: $badges")
        
        badgeCriteriaList.forEach { criteria ->
            val meetsCriteria = criteria.check(userStats)
            Log.d("BadgeDebug", "Checking ${criteria.type.displayName}: meets criteria = $meetsCriteria")
            
            if (!badges.contains(criteria.type.key) && meetsCriteria) {
                Log.d("BadgeDebug", "Awarding badge: ${criteria.type.displayName}")
                onBadgeAwarded(criteria.type)
            }
        }
    }
}
