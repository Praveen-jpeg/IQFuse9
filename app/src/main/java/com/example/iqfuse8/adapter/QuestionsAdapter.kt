package com.example.iqfuse8.adapter

import android.graphics.Color
import android.text.Html
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

        // Reset buttons' states
        buttons.forEach { button ->
            button.visibility = View.GONE
            button.isEnabled = true
            button.setBackgroundColor(Color.LTGRAY)
        }
        holder.explanationText.visibility = View.GONE

        // Set buttons for available options
        options.forEachIndexed { index, option ->
            if (index < buttons.size) {
                val button = buttons[index]
                button.text = option
                button.visibility = View.VISIBLE

                button.setOnClickListener {
                    // Allow user to try until correct answer is selected
                    if (!button.isEnabled) return@setOnClickListener

                    val selectedAnswer = option
                    if (selectedAnswer == question.answer) {
                        // Correct answer: mark this button green, disable all options, show explanation and encouragement
                        button.setBackgroundColor(Color.GREEN)
                        Toast.makeText(holder.itemView.context, "Correct!", Toast.LENGTH_SHORT).show()

                        // Disable all option buttons
                        buttons.forEach { it.isEnabled = false }

                        // Show explanation and encouragement message
                        holder.explanationText.visibility = View.VISIBLE
                        holder.explanationText.text = "Correct Answer: ${question.answer}\nExplanation: ${question.explanation}\n" } else {
                        // Wrong answer: mark this button red, disable only this button
                        button.setBackgroundColor(Color.RED)
                        button.isEnabled = false
                        Toast.makeText(holder.itemView.context, "Wrong! Try again.", Toast.LENGTH_SHORT).show()
                        // Optionally, show a partial message if desired; here we let the user try again
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = questionsList.size
}
