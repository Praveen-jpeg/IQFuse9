package com.example.iqfuse8

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.iqfuse8.adapter.QuestionsAdapter
import com.example.iqfuse8.model.Question
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class QuestionsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: QuestionsAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var btnNextSet: Button
    private lateinit var btnPreviousSet: Button
    private lateinit var topicNameTextView: TextView
    private var topic: String? = null
    private var currentSet = 1
    private val questionsList = mutableListOf<Question>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questions)

        recyclerView = findViewById(R.id.recyclerViewQuestions)
        btnNextSet = findViewById(R.id.btnNextSet)
        btnPreviousSet = findViewById(R.id.btnPreviousSet)
        topicNameTextView = findViewById(R.id.topicTitle)

        db = Firebase.firestore

        topic = intent.getStringExtra("topic")
        topicNameTextView.text = topic ?: "Questions"

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = QuestionsAdapter(questionsList)
        recyclerView.adapter = adapter

        fetchQuestions()

        btnNextSet.setOnClickListener {
            if (currentSet < 4) {
                currentSet++
                fetchQuestions()
            } else {
                Toast.makeText(this, "No more sets available", Toast.LENGTH_SHORT).show()
            }
        }

        btnPreviousSet.setOnClickListener {
            if (currentSet > 1) {
                currentSet--
                fetchQuestions()
            } else {
                Toast.makeText(this, "You're at the first set", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchQuestions() {
        topic?.let { topicName ->
            val path = "questions/$topicName/sets/Set $currentSet"
            Log.d("QuestionsActivity", "Fetching from: $path")

            db.collection("questions").document(topicName)
                .collection("sets")
                .document("Set $currentSet")
                .get()
                .addOnSuccessListener { document ->
                    Log.d("QuestionsActivity", "Document exists: ${document.exists()}")

                    if (document.exists()) {
                        val questionList = document.get("questions") as? List<Map<String, Any>>
                        Log.d("QuestionsActivity", "Number of Questions ${questionList?.size}")

                        if (questionList != null && questionList.isNotEmpty()) {
                            questionsList.clear()
                            questionList.forEach { questionData ->
                                val questionText = questionData["question"] as? String ?: ""
                                val optionsList = questionData["options"] as? List<String> ?: emptyList()
                                val answer = questionData["answer"] as? String ?: ""
                                val explanation = questionData["explanation"] as? String ?: ""

                                questionsList.add(Question(questionText, optionsList, answer, explanation))
                            }

                            questionsList.shuffle()
                            adapter = QuestionsAdapter(questionsList)
                            recyclerView.adapter = adapter
                        } else {
                            Log.e("QuestionsActivity", "Document found but no questions")
                            Toast.makeText(this, "No questions available in this set", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("QuestionsActivity", "No such document: $path")
                        Toast.makeText(this, "No questions found for this topic & set", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("QuestionsActivity", "Error fetching questions", exception)
                }
        }
    }

}
