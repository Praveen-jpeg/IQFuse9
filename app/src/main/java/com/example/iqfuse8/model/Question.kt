package com.example.iqfuse8.model

data class Question(
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "", // Match Firestore field name
    val explanation: String = ""
)
