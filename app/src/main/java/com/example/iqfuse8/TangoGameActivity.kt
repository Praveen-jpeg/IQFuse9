package com.example.iqfuse8

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class TangoGameActivity : AppCompatActivity() {
    private lateinit var gridLayout: GridLayout
    private lateinit var puzzle: Array<CharArray>
    private lateinit var userGrid: Array<CharArray>
    private var puzzleSize = 6 
    private lateinit var buttons: Array<Array<Button>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tango_game)

        gridLayout = findViewById(R.id.gridLayout)
        val checkButton: Button = findViewById(R.id.btnCheckSolution)

        setupGrid()
        loadPuzzle()

        checkButton.setOnClickListener { checkSolution() }
    }

    private fun setupGrid() {
        gridLayout.apply {
            columnCount = puzzleSize
            rowCount = puzzleSize
        }

        buttons = Array(puzzleSize) { row ->
            Array(puzzleSize) { col ->
                Button(this).apply {
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = 200
                        height = 200
                        setMargins(8, 8, 8, 8)
                        gravity = Gravity.CENTER
                    }
                    setOnClickListener { onCellClicked(row, col) }
                    gridLayout.addView(this)
                }
            }
        }
    }

    private fun loadPuzzle() {
        puzzle = PuzzleGenerator.getPuzzleForDay(puzzleSize)
        userGrid = Array(puzzleSize) { puzzle[it].clone() }

        for (row in 0 until puzzleSize) {
            for (col in 0 until puzzleSize) {
                buttons[row][col].text = if (puzzle[row][col] == ' ') "" else puzzle[row][col].toString()
            }
        }
    }

    private fun onCellClicked(row: Int, col: Int) {
        Log.d("TangoGame", "Cell clicked: ($row, $col)")

        if (puzzle[row][col] != ' ') {
            Log.d("TangoGame", "Cell ($row, $col) is fixed, can't change.")
            return // Prevent changing preset values
        }

        val newSymbol = when (userGrid[row][col]) {
            ' ' -> '☀'   // Sun
            '☀' -> '☾'   // Moon
            '☾' -> ' '   // Empty
            else -> ' '
        }

        Log.d("TangoGame", "Changing cell ($row, $col) to $newSymbol")

        // Temporarily disable move validation for debugging
        userGrid[row][col] = newSymbol
        buttons[row][col].text = newSymbol.toString()

        // If validation is needed, uncomment the following:

        if (isMoveValid(row, col, newSymbol)) {
            userGrid[row][col] = newSymbol
            buttons[row][col].text = newSymbol.toString()
            Log.d("TangoGame", "Move applied successfully!")
        } else {
            Toast.makeText(this, "Invalid Move!", Toast.LENGTH_SHORT).show()
            Log.d("TangoGame", "Move rejected.")
        }

    }

    private fun isMoveValid(row: Int, col: Int, symbol: Char): Boolean {
        return checkNoThreeInARowOrColumn(row, col, symbol) &&
                checkBalancedRowAndColumn()
    }

    private fun checkNoThreeInARowOrColumn(row: Int, col: Int, symbol: Char): Boolean {
        val grid = userGrid.map { it.clone() }.toTypedArray()
        grid[row][col] = symbol

        fun hasThreeConsecutive(arr: CharArray): Boolean {
            for (i in 0 until arr.size - 2) {
                if (arr[i] == arr[i + 1] && arr[i] == arr[i + 2] && arr[i] != ' ') return true
            }
            return false
        }

        return !hasThreeConsecutive(grid[row]) &&
                !hasThreeConsecutive(grid.map { it[col] }.toCharArray())
    }

    private fun checkBalancedRowAndColumn(): Boolean {
        for (i in 0 until puzzleSize) {
            val sunRowCount = userGrid[i].count { it == '☀' }
            val moonRowCount = userGrid[i].count { it == '☾' }
            val sunColCount = userGrid.map { it[i] }.count { it == '☀' }
            val moonColCount = userGrid.map { it[i] }.count { it == '☾' }

            if (sunRowCount > puzzleSize / 2 || moonRowCount > puzzleSize / 2) return false
            if (sunColCount > puzzleSize / 2 || moonColCount > puzzleSize / 2) return false
        }
        return true
    }

    private fun checkSolution() {
        if (checkBalancedRowAndColumn()) {
            Toast.makeText(this, "Correct Solution!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Incorrect Solution!", Toast.LENGTH_SHORT).show()
        }
    }
}
