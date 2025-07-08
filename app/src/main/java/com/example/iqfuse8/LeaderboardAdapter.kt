package com.example.iqfuse8

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {
    private var entries: List<LeaderboardEntry> = emptyList()

    data class LeaderboardEntry(
        val userId: String,
        val displayName: String,
        val completionTime: Int,
        val timestamp: Long
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankText: TextView = view.findViewById(R.id.tvRank)
        val medalIcon: ImageView = view.findViewById(R.id.ivMedal)
        val nameText: TextView = view.findViewById(R.id.tvName)
        val timeText: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context
        
        // Set rank number
        holder.rankText.text = "#${position + 1}"
        
        // Apply styling based on position
        when (position) {
            0 -> { // 1st place
                holder.rankText.background = ContextCompat.getDrawable(context, R.drawable.gold_rank_circle)
                holder.medalIcon.visibility = View.VISIBLE
                holder.medalIcon.setImageResource(R.drawable.gold_medal)
            }
            1 -> { // 2nd place
                holder.rankText.background = ContextCompat.getDrawable(context, R.drawable.silver_rank_circle)
                holder.medalIcon.visibility = View.VISIBLE
                holder.medalIcon.setImageResource(R.drawable.silver_medal)
            }
            2 -> { // 3rd place
                holder.rankText.background = ContextCompat.getDrawable(context, R.drawable.bronze_rank_circle)
                holder.medalIcon.visibility = View.VISIBLE
                holder.medalIcon.setImageResource(R.drawable.bronze_medal)
            }
            else -> {
                holder.rankText.background = ContextCompat.getDrawable(context, R.drawable.rank_circle_background)
                holder.medalIcon.visibility = View.GONE
            }
        }
        
        // Set player name (handle empty names)
        if (entry.displayName.isBlank()) {
            holder.nameText.text = "Anonymous Player"
        } else {
            holder.nameText.text = entry.displayName
        }
        
        // Set completion time
        holder.timeText.text = formatTime(entry.completionTime)
    }

    override fun getItemCount() = entries.size

    fun updateEntries(newEntries: List<LeaderboardEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }
} 