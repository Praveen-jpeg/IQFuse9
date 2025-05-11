package com.example.iqfuse8

import com.google.firebase.firestore.FirebaseFirestore

object UserStatsManager {

    private fun getFirestore() = FirebaseFirestore.getInstance()

    fun updateUserStats(userId: String, correct: Boolean) {
        val userDocRef = getFirestore().collection("users").document(userId)

        getFirestore().runTransaction { transaction ->
            val snapshot = transaction.get(userDocRef)
            
            val totalCompleted = snapshot.getLong("totalChallengesCompleted")?.toInt() ?: 0
            val correctAnswersInRow = snapshot.getLong("correctAnswersInRow")?.toInt() ?: 0

            val updatedStats = if (correct) {
                mapOf(
                    "totalChallengesCompleted" to (totalCompleted + 1),
                    "correctAnswersInRow" to (correctAnswersInRow + 1)
                )
            } else {
                mapOf(
                    "totalChallengesCompleted" to (totalCompleted + 1),
                    "correctAnswersInRow" to 0
                )
            }

            transaction.update(userDocRef, updatedStats)
        }.addOnSuccessListener {
            println("User stats updated successfully")
        }.addOnFailureListener { e ->
            println("Error updating user stats: ${e.message}")
        }
    }
}
