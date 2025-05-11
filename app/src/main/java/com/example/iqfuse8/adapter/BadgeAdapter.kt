package com.example.iqfuse8.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.iqfuse8.*

class BadgeAdapter(private val badges: List<Badge>) :
    RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    // ViewHolder for each badge item
    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val badgeIcon: ImageView = itemView.findViewById(R.id.badgeIcon)
        val badgeName: TextView = itemView.findViewById(R.id.badgeTitle)
        val badgeDescription: TextView = itemView.findViewById(R.id.badgeDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        // Inflate the badge item layout
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.badge_item, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = badges[position]
        val context = holder.itemView.context

        // Check if any badges are earned
        val hasAnyEarnedBadges = badges.any { it.earned }

        if (!hasAnyEarnedBadges) {
            // If no badges are earned, show locked icon for all badges
            holder.badgeIcon.setImageResource(R.drawable.ic_locked)
            holder.badgeIcon.alpha = 0.3f
        } else {
            // If some badges are earned, show appropriate icons
            val resId = when (badge.type) {
                BadgeType.FIRST_WIN -> R.drawable.ic_firstwin
                BadgeType.CENTURY_SOLVER -> R.drawable.ic_100
                BadgeType.STREAK_ROOKIE -> R.drawable.ic_3
                BadgeType.CONSISTENCY_STAR -> R.drawable.ic_7
                BadgeType.DEDICATION_CHAMP -> R.drawable.ic_15
                BadgeType.MASTER_STREAKER -> R.drawable.ic_30
                BadgeType.UNBREAKABLE -> R.drawable.ic_100
                BadgeType.SHARP_SHOOTER -> R.drawable.ic_5
                BadgeType.FLAWLESS_WEEK -> R.drawable.ic_seven
                BadgeType.STREAK_SAVIOR -> R.drawable.ic_streaksavior
                BadgeType.COMEBACK_KING -> R.drawable.ic_comeback
            }

            // Set the badge image
            holder.badgeIcon.setImageResource(if (badge.earned) resId else R.drawable.ic_locked)
            holder.badgeIcon.alpha = if (badge.earned) 1.0f else 0.3f
        }

        // Set the badge name and description
        holder.badgeName.text = badge.type.displayName
        holder.badgeDescription.text = badge.type.description
        holder.badgeDescription.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int = badges.size
}
