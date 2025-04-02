package com.example.iqfuse8
object PuzzleGenerator {
    fun getPuzzleForDay(size: Int): Array<CharArray> {
        return when (size) {
            4 -> arrayOf(
                charArrayOf(' ', '☀', ' ', '☾'),
                charArrayOf('☾', ' ', '☀', ' '),
                charArrayOf(' ', '☾', ' ', '☀'),
                charArrayOf('☀', ' ', '☾', ' ')
            )
            6 -> arrayOf(
                charArrayOf(' ', '☀', ' ', '☾', ' ', '☀'),
                charArrayOf('☾', ' ', '☀', ' ', '☀', ' '),
                charArrayOf(' ', '☾', ' ', '☀', ' ', '☾'),
                charArrayOf('☀', ' ', '☾', ' ', '☾', ' '),
                charArrayOf(' ', '☀', ' ', '☀', ' ', '☾'),
                charArrayOf('☾', ' ', '☀', ' ', '☾', ' ')
            )
            else -> throw IllegalArgumentException("Unsupported puzzle size")
        }
    }
}
