package com.example.iqfuse8

import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class TangoGameActivity : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var checkButton: Button
    private lateinit var streakTextView: TextView
    private lateinit var scoreTextView: TextView
    private val size = 6
    private lateinit var puzzle: Array<CharArray>
    private lateinit var cells: Array<Array<TextView>>

    private var score = 0
    private var streak = 0
    private val prefs by lazy { getSharedPreferences("TangoPrefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tango_game)

        gridLayout = findViewById(R.id.gridLayout)
        checkButton = findViewById(R.id.btnCheckSolution)
        streakTextView = findViewById(R.id.tvStreak)
        scoreTextView = findViewById(R.id.tvScore)

        puzzle = PuzzleGenerator.getPuzzleForDay(size)
        cells = Array(size) { Array(size) { TextView(this) } }

        loadScoreAndStreak()
        updateScoreAndStreakUI()
        setupGrid()

        checkButton.setOnClickListener {
            if (validatePuzzle()) {
                score += 10
                streak += 1
                saveScoreAndStreak()
                updateScoreAndStreakUI()
                Toast.makeText(this, "✅ Puzzle Valid! +10 Points!", Toast.LENGTH_SHORT).show()
            } else {
                streak = 0
                saveScoreAndStreak()
                updateScoreAndStreakUI()
                Toast.makeText(this, "❌ Invalid Puzzle! Streak Reset!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupGrid() {
        gridLayout.columnCount = size
        gridLayout.rowCount = size
        gridLayout.removeAllViews()

        for (i in 0 until size) {
            for (j in 0 until size) {
                val cell = TextView(this)
                cell.text = puzzle[i][j].toString()
                cell.textSize = 24f
                cell.setPadding(12, 12, 12, 12)
                cell.setBackgroundResource(R.drawable.cell_border)
                cell.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                cell.setTextColor(ContextCompat.getColor(this, android.R.color.black))
                cell.setOnClickListener {
                    if (puzzle[i][j] == ' ') {
                        toggleCell(cell)
                    }
                }
                cells[i][j] = cell
                val params = GridLayout.LayoutParams()
                params.rowSpec = GridLayout.spec(i, 1, 1f)
                params.columnSpec = GridLayout.spec(j, 1, 1f)
                params.width = 0
                params.height = 0
                gridLayout.addView(cell, params)
            }
        }
    }

    private fun toggleCell(cell: TextView) {
        cell.text = when (cell.text) {
            "☀" -> "☾"
            "☾" -> " "
            else -> "☀"
        }
    }

    private fun validatePuzzle(): Boolean {
        // Example validation: each row and column must contain equal number of ☀ and ☾ (i.e., 3 of each in 6x6)
        for (i in 0 until size) {
            val rowSymbols = cells[i].map { it.text }
            val colSymbols = cells.map { it[i].text }

            if (!isBalanced(rowSymbols) || !isBalanced(colSymbols)) return false
        }
        return true
    }

    private fun isBalanced(symbols: List<CharSequence>): Boolean {
        val sun = symbols.count { it == "☀" }
        val moon = symbols.count { it == "☾" }
        return sun == moon
    }

    private fun loadScoreAndStreak() {
        score = prefs.getInt("score", 0)
        streak = prefs.getInt("streak", 0)
    }

    private fun saveScoreAndStreak() {
        prefs.edit().putInt("score", score).putInt("streak", streak).apply()
    }

    private fun updateScoreAndStreakUI() {
        scoreTextView.text = "Score: $score"
        streakTextView.text = "🔥 Streak: $streak"
    }
}
