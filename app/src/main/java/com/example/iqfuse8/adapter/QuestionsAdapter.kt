package com.example.iqfuse8.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.iqfuse8.R
import com.example.iqfuse8.model.Question

class QuestionsAdapter(private val questionsList: List<Question>) :
    RecyclerView.Adapter<QuestionsAdapter.QuestionViewHolder>() {

    class QuestionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionText: TextView = view.findViewById(R.id.questionTextView)
        val option1: Button = view.findViewById(R.id.option1)
        val option2: Button = view.findViewById(R.id.option2)
        val option3: Button = view.findViewById(R.id.option3)
        val option4: Button = view.findViewById(R.id.option4)
        val explanationText: TextView = view.findViewById(R.id.explanationTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return QuestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        val question = questionsList[position]
        holder.questionText.text = question.questionText

        // Assign options dynamically
        val options = question.options
        val buttons = listOf(holder.option1, holder.option2, holder.option3, holder.option4)

        buttons.forEachIndexed { index, button ->
            if (index < options.size) {
                button.text = options[index]
                button.visibility = View.VISIBLE

                button.setOnClickListener {
                    val selectedAnswer = options[index]
                    if (selectedAnswer == question.answer) {
                        button.setBackgroundColor(Color.GREEN)
                        Toast.makeText(holder.itemView.context, "Correct!", Toast.LENGTH_SHORT).show()
                    } else {
                        button.setBackgroundColor(Color.RED)
                        Toast.makeText(holder.itemView.context, "Wrong!", Toast.LENGTH_SHORT).show()
                    }

                    // Show explanation
                    holder.explanationText.text = question.explanation
                    holder.explanationText.visibility = View.VISIBLE
                }
            } else {
                button.visibility = View.GONE
            }
        }

        // Hide explanation initially
        holder.explanationText.visibility = View.GONE
    }

    override fun getItemCount(): Int = questionsList.size
}
