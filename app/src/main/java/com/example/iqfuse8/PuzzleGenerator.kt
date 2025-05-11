package com.example.iqfuse8

import java.util.*

object PuzzleGenerator {

    fun getPuzzleForDay(size: Int): Array<CharArray> {
        // Use the current date to generate a consistent puzzle for the day
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        return when (size) {
            4 -> get4x4Puzzle(dayOfYear)
            6 -> get6x6Puzzle(dayOfYear)
            else -> throw IllegalArgumentException("Unsupported puzzle size")
        }
    }

    private fun get4x4Puzzle(seed: Int): Array<CharArray> {
        val puzzles = listOf(
            arrayOf(
                charArrayOf(' ', '☀', ' ', '☾'),
                charArrayOf('☾', ' ', '☀', ' '),
                charArrayOf(' ', '☾', ' ', '☀'),
                charArrayOf('☀', ' ', '☾', ' ')
            ),
            arrayOf(
                charArrayOf('☀', ' ', '☾', ' '),
                charArrayOf(' ', '☾', ' ', '☀'),
                charArrayOf('☾', ' ', '☀', ' '),
                charArrayOf(' ', '☀', ' ', '☾')
            ),
            arrayOf(
                charArrayOf(' ', '☾', '☀', ' '),
                charArrayOf('☀', ' ', ' ', '☾'),
                charArrayOf('☾', ' ', ' ', '☀'),
                charArrayOf(' ', '☀', '☾', ' ')
            )
        )
        return puzzles[seed % puzzles.size]
    }

    private fun get6x6Puzzle(seed: Int): Array<CharArray> {
        val puzzles = listOf(
            arrayOf(
                charArrayOf(' ', '☀', ' ', '☾', ' ', '☀'),
                charArrayOf('☾', ' ', '☀', ' ', '☀', ' '),
                charArrayOf(' ', '☾', ' ', '☀', ' ', '☾'),
                charArrayOf('☀', ' ', '☾', ' ', '☾', ' '),
                charArrayOf(' ', '☀', ' ', '☀', ' ', '☾'),
                charArrayOf('☾', ' ', '☀', ' ', '☾', ' ')
            ),
            arrayOf(
                charArrayOf('☀', ' ', '☾', ' ', '☀', ' '),
                charArrayOf(' ', '☾', ' ', '☀', ' ', '☾'),
                charArrayOf('☾', ' ', '☀', ' ', '☾', ' '),
                charArrayOf(' ', '☀', ' ', '☾', ' ', '☀'),
                charArrayOf('☀', ' ', '☾', ' ', '☀', ' '),
                charArrayOf(' ', '☾', ' ', '☀', ' ', '☾')
            ),
            arrayOf(
                charArrayOf(' ', '☾', '☀', ' ', '☾', ' '),
                charArrayOf('☀', ' ', ' ', '☾', ' ', '☀'),
                charArrayOf('☾', ' ', '☀', ' ', '☀', ' '),
                charArrayOf(' ', '☀', ' ', '☾', ' ', '☾'),
                charArrayOf('☀', ' ', '☾', ' ', '☀', ' '),
                charArrayOf(' ', '☾', ' ', '☀', ' ', '☀')
            ),
            // New pattern added for variety and balance
            arrayOf(
                charArrayOf('☀', ' ', '☾', ' ', '☀', ' '),
                charArrayOf('☾', '☀', ' ', '☾', ' ', '☀'),
                charArrayOf(' ', '☾', '☀', ' ', '☀', ' '),
                charArrayOf('☀', ' ', '☾', '☀', ' ', ' '),
                charArrayOf('☾', ' ', '☀', ' ', '☾', '☀'),
                charArrayOf(' ', '☀', ' ', '☾', '☀', ' ')
            )
        )
        return puzzles[seed % puzzles.size]
    }
}
