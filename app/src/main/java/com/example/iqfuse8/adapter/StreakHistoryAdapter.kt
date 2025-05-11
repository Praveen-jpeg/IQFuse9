package com.example.iqfuse8.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.iqfuse8.R
import com.example.iqfuse8.model.StreakHistory

class StreakHistoryAdapter(private val history: List<StreakHistory>) : 
    RecyclerView.Adapter<StreakHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvStreak: TextView = view.findViewById(R.id.tvStreak)
        val tvReason: TextView = view.findViewById(R.id.tvReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_streak_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = history[position]
        holder.tvDate.text = item.date
        holder.tvStreak.text = "Streak: ${item.streak}"
        
        val reasonText = when(item.reason) {
            "correct" -> "Correct Answer"
            "wrong" -> "Wrong Answer"
            "missed" -> "Missed Day"
            "milestone" -> "Streak Milestone"
            else -> item.reason
        }
        holder.tvReason.text = reasonText
    }

    override fun getItemCount() = history.size
} 