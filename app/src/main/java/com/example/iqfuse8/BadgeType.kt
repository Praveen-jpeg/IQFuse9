package com.example.iqfuse8

enum class BadgeType(val key: String, val displayName: String, val description: String) {
    FIRST_WIN("first_win", "First Win", "Completed your first challenge!"),
    CENTURY_SOLVER("century_solver", "Century Solver", "Completed 100 challenges!"),
    STREAK_ROOKIE("streak_rookie", "Streak Rookie", "Maintained a 3-day streak."),
    CONSISTENCY_STAR("consistency_star", "Consistency Star", "7-day streak! You're consistent!"),
    DEDICATION_CHAMP("dedication_champ", "Dedication Champ", "15-day streak — impressive!"),
    MASTER_STREAKER("master_streaker", "Master Streaker", "30-day streak — elite level!"),
    UNBREAKABLE("unbreakable", "Unbreakable", "100-day streak! Unstoppable!"),
    SHARP_SHOOTER("sharp_shooter", "Sharp Shooter", "5 correct answers in a row."),
    FLAWLESS_WEEK("flawless_week", "Flawless Week", "7-day streak with 100% correct answers."),
    STREAK_SAVIOR("streak_savior", "Streak Savior", "Saved your streak from breaking!"),
    COMEBACK_KING("comeback_king", "Comeback King", "Returned after a break and maintained consistency!")
}
