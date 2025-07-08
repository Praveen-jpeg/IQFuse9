package com.example.iqfuse8

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

class TangoGameActivity : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var checkButton: Button
    private lateinit var hintButton: Button
    private lateinit var undoButton: Button
    private lateinit var streakTextView: TextView
    private lateinit var scoreTextView: TextView
    private val size = 6 // Grid size (6x6)
    private lateinit var puzzle: Array<CharArray> // Puzzle layout (partial)
    private lateinit var cells: Array<Array<TextView>> // Grid cells
    private lateinit var horizontalConstraints: Array<Array<Char>> // Horizontal constraints
    private lateinit var verticalConstraints: Array<Array<Char>> // Vertical constraints

    private var score = 0
    private var streak = 0
    private val prefs by lazy { getSharedPreferences("TangoPrefs", MODE_PRIVATE) }
    private val auth: FirebaseAuth get() = FirebaseManager.auth
    private var isCompleted = false
    private var moveHistory = mutableListOf<Pair<Int, Int>>() // Track moves for undo
    private val TAG = "TangoGameActivity"

    // List of initially visible cells
    private var initialVisibleCells = mutableListOf<Pair<Int, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tango_game)

        // Initialize Firebase components
        FirebaseManager.initialize(applicationContext)

        // Initialize views
        gridLayout = findViewById(R.id.gridLayout)
        checkButton = findViewById(R.id.btnCheckSolution)
        streakTextView = findViewById(R.id.tvStreak)
        scoreTextView = findViewById(R.id.tvScore)
        hintButton = findViewById(R.id.btnHint)
        undoButton = findViewById(R.id.btnUndo)

        // Initialize cells array
        cells = Array(size) { Array(size) { TextView(this) } }

        // Initialize empty constraints initially
        horizontalConstraints = Array(size - 1) { Array(size) { ' ' } }
        verticalConstraints = Array(size) { Array(size - 1) { ' ' } }

        // Load saved data
        loadScoreAndStreak()
        updateScoreAndStreakUI()

        // Setup UI elements
        setupButtons()

        // Initialize Firebase auth and check authentication
        handleAuthentication()
    }

    private fun handleAuthentication() {
        // For now, sign in anonymously if not already authenticated
        FirebaseManager.signInAnonymously { success ->
            if (success) {
                Log.d(TAG, "Authentication successful")
                // Check if user has completed today's puzzle
                checkCompletionStatus()
                // Initialize puzzles and fetch today's puzzle
                initializeAndFetchPuzzle()
            } else {
                Log.e(TAG, "Authentication failed")
                // Still try to fetch puzzle even if auth fails
                initializeAndFetchPuzzle()
            }
        }
    }

    private fun initializeAndFetchPuzzle() {
        // Log Tango puzzles for debugging
        FirestoreDebugger.logTangoPuzzles()
        
        // First force upload puzzles to ensure they're in Firestore
        Toast.makeText(this, "Initializing puzzles...", Toast.LENGTH_SHORT).show()
        
        FirebaseManager.forceUploadTangoPuzzles { success ->
            if (success) {
                Log.d(TAG, "Successfully uploaded puzzles to Firestore")
                
                // Now fetch today's puzzle
                runOnUiThread {
                    fetchTodaysPuzzle()
                }
            } else {
                Log.e(TAG, "Failed to upload puzzles to Firestore")
                
                // Use local puzzles as fallback
                runOnUiThread {
                    Toast.makeText(this, "Using local puzzles", Toast.LENGTH_SHORT).show()
                    puzzle = PuzzleGenerator.getPartialPuzzleForDay(size)
                    setupConstraints()
                    setupGrid()
                }
            }
        }
    }

    private fun fetchTodaysPuzzle() {
        Log.d(TAG, "Fetching today's puzzle")
        
        // Show loading indicator
        Toast.makeText(this, "Loading puzzle...", Toast.LENGTH_SHORT).show()
        
        PuzzleGenerator.getPuzzleFromFirestore { partialGrid, puzzleModel ->
            if (partialGrid != null && puzzleModel != null) {
                Log.d(TAG, "Successfully fetched puzzle: ${puzzleModel.id}")
                
                // Store the partial puzzle for display
                puzzle = partialGrid
                
                // Store initially visible cells to prevent user from changing them
                initialVisibleCells = puzzleModel.visibleCells.toMutableList()
                
                // Debug log for visible cells
                initialVisibleCells.forEach { (row, col) ->
                    Log.d(TAG, "Visible cell at [$row][$col] = '${if (row < puzzle.size && col < puzzle[row].size) puzzle[row][col] else "out of bounds"}'")
                }
                
                // Convert constraint strings to arrays
                horizontalConstraints = TangoPuzzleModel.constraintsToCharArray(puzzleModel.horizontalConstraints)
                verticalConstraints = TangoPuzzleModel.constraintsToCharArray(puzzleModel.verticalConstraints)
                
                // Update UI with puzzle data
                runOnUiThread {
                    setupGrid()
                    
                    // Confirm visibility to user
                    Toast.makeText(this, "Puzzle loaded with ${initialVisibleCells.size} visible cells", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Failed to fetch puzzle, using local puzzle")
                // Fallback to local puzzle
                puzzle = PuzzleGenerator.getPartialPuzzleForDay(size)
                setupConstraints() // Use default constraints
                runOnUiThread {
                    setupGrid()
                }
            }
        }
    }

    private fun checkCompletionStatus() {
        if (FirebaseManager.currentUser != null) {
            PuzzleGenerator.checkPuzzleCompletionStatus(FirebaseManager.currentUser!!.uid) { completed ->
                isCompleted = completed
                if (completed) {
                    runOnUiThread {
                        Toast.makeText(this, "You've already completed today's puzzle!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        // Setup check button
        checkButton.setOnClickListener {
            checkSolution()
        }

        // Setup hint button
        hintButton.setOnClickListener {
            // Provide a hint by revealing a randomly empty cell
            provideHint()
        }

        // Setup undo button
        undoButton.setOnClickListener {
            // Undo the last move
            undoLastMove()
        }
    }

    private fun checkSolution() {
        // First check using regular validation rules
        if (!validatePuzzleRules()) {
            Toast.makeText(this, "Your solution doesn't follow the game rules. Try again!", Toast.LENGTH_SHORT).show()
            return
        }

        // Then check against the correct solution from the database
        val isCorrect = PuzzleGenerator.validateUserSolution(cells)

        if (isCorrect) {
            Toast.makeText(this, "Correct solution!", Toast.LENGTH_SHORT).show()
            score += 100
            streak++
            saveScoreAndStreak()
            updateScoreAndStreakUI()

            // Mark as completed in Firestore if authenticated
            if (FirebaseManager.currentUser != null) {
                PuzzleGenerator.markPuzzleAsCompleted(FirebaseManager.currentUser!!.uid)
                isCompleted = true

                // Show completion dialog
                showCompletionDialog()
            }
        } else {
            Toast.makeText(this, "Your solution is incorrect. Try again!", Toast.LENGTH_SHORT).show()
            streak = 0
            saveScoreAndStreak()
            updateScoreAndStreakUI()
        }
    }

    private fun setupConstraints() {
        // Initialize with empty constraints (only used as fallback if Firestore fails)
        horizontalConstraints = Array(size - 1) { Array(size) { ' ' } }
        verticalConstraints = Array(size) { Array(size - 1) { ' ' } }

        // Set sample constraints based on the first day's puzzle
        // '=' means same symbols, 'x' means different symbols
        horizontalConstraints[3][0] = '='
        horizontalConstraints[3][2] = '='
        horizontalConstraints[4][0] = 'x'
        horizontalConstraints[4][4] = 'x'
        horizontalConstraints[5][1] = '='
        horizontalConstraints[5][3] = '='

        verticalConstraints[1][2] = 'x'
        verticalConstraints[3][4] = 'x'
    }

    private fun setupGrid() {
        Log.d(TAG, "Setting up grid with ${initialVisibleCells.size} visible cells")
        gridLayout.removeAllViews()
        gridLayout.columnCount = size
        gridLayout.rowCount = size

        // Calculate cell size to fill most of the screen width
        val screenWidth = resources.displayMetrics.widthPixels
        val margins = 32 // Total horizontal margins (16dp on each side)
        val cellSize = (screenWidth - margins) / size // Equal distribution of space

        val sunSymbol = "\u2600" // ☀
        val moonSymbol = "\u263E" // ☾

        for (i in 0 until size) {
            for (j in 0 until size) {
                // Check if this cell is initially visible
                val isInitiallyVisible = initialVisibleCells.contains(Pair(i, j))
                
                // Debug log
                if (isInitiallyVisible) {
                    Log.d(TAG, "Cell [$i][$j] is initially visible with '${if (i < puzzle.size && j < puzzle[i].size) puzzle[i][j] else "out of bounds"}'")
                }
                
                val cellContent = when {
                    i < puzzle.size && j < puzzle[i].size && puzzle[i][j] == '☀' -> sunSymbol
                    i < puzzle.size && j < puzzle[i].size && puzzle[i][j] == '☾' -> moonSymbol
                    else -> ""
                }

                val cell = TextView(this).apply {
                    text = cellContent
                    textSize = 24f
                    width = cellSize
                    height = cellSize
                    setPadding(4, 4, 4, 4)
                    
                    // Set different text colors for prefilled vs user-filled cells
                    if (isInitiallyVisible) {
                        // Prefilled cells shown in bold black
                        setTextColor(Color.BLACK)
                        textSize = 26f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        // Add a background to make prefilled cells stand out more
                        setBackgroundColor(Color.parseColor("#F0F0F0"))
                    } else {
                        // User-filled cells in blue
                        setTextColor(ContextCompat.getColor(this@TangoGameActivity, R.color.purple_500))
                        // Normal background
                        background = ContextCompat.getDrawable(this@TangoGameActivity, R.drawable.cell_border)
                    }
                    
                    textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                    gravity = android.view.Gravity.CENTER

                    // Only make cells clickable if they weren't initially visible
                    if (!isInitiallyVisible) {
                        setOnClickListener {
                            if (!isCompleted) {
                                toggleCell(this, i, j)
                            } else {
                                Toast.makeText(this@TangoGameActivity, "You've already completed today's puzzle!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                cells[i][j] = cell

                val params = GridLayout.LayoutParams().apply {
                    rowSpec = GridLayout.spec(i, 1f)
                    columnSpec = GridLayout.spec(j, 1f)
                    width = cellSize
                    height = cellSize
                }
                
                gridLayout.addView(cell, params)
                
                // Add constraint markers between cells if needed
                if (j < size - 1) {
                    addHorizontalConstraint(i, j, cellSize)
                }
                if (i < size - 1) {
                    addVerticalConstraint(i, j, cellSize)
                }
            }
        }
    }

    private fun addHorizontalConstraint(row: Int, col: Int, cellSize: Int) {
        if (row < horizontalConstraints.size && col < horizontalConstraints[row].size && horizontalConstraints[row][col] != ' ') {
            val constraintView = TextView(this).apply {
                text = if (horizontalConstraints[row][col] == '=') "=" else "×"
                textSize = 16f
                width = cellSize / 4
                height = cellSize / 4
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                gravity = android.view.Gravity.CENTER
                setTextColor(if (horizontalConstraints[row][col] == '=') Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            }

            val params = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(row + 1, 1f)
                columnSpec = GridLayout.spec(col, 1f)
                width = cellSize / 4
                height = cellSize / 4
                setMargins(cellSize / 2, -cellSize / 8, 0, 0)
            }

            gridLayout.addView(constraintView, params)
        }
    }

    private fun addVerticalConstraint(row: Int, col: Int, cellSize: Int) {
        if (row < verticalConstraints.size && col < verticalConstraints[row].size && verticalConstraints[row][col] != ' ') {
            val constraintView = TextView(this).apply {
                text = if (verticalConstraints[row][col] == '=') "=" else "×"
                textSize = 16f
                width = cellSize / 4
                height = cellSize / 4
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                gravity = android.view.Gravity.CENTER
                setTextColor(if (verticalConstraints[row][col] == '=') Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
            }

            val params = GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(row, 1f)
                columnSpec = GridLayout.spec(col + 1, 1f)
                width = cellSize / 4
                height = cellSize / 4
                setMargins(-cellSize / 8, cellSize / 2, 0, 0)
            }

            gridLayout.addView(constraintView, params)
        }
    }

    private fun toggleCell(cell: TextView, row: Int, col: Int) {
        val sunSymbol = "\u2600" // ☀
        val moonSymbol = "\u263E" // ☾

        // Add to move history before changing
        moveHistory.add(Pair(row, col))

        when (cell.text.toString()) {
            "" -> cell.text = sunSymbol
            sunSymbol -> cell.text = moonSymbol
            moonSymbol -> cell.text = ""
        }
    }

    private fun undoLastMove() {
        if (moveHistory.isNotEmpty()) {
            val lastMove = moveHistory.removeAt(moveHistory.size - 1)
            val cell = cells[lastMove.first][lastMove.second]
            val sunSymbol = "\u2600" // ☀
            val moonSymbol = "\u263E" // ☾

            when (cell.text.toString()) {
                "" -> cell.text = moonSymbol
                sunSymbol -> cell.text = ""
                moonSymbol -> cell.text = sunSymbol
            }
        } else {
            Toast.makeText(this, "No moves to undo!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun provideHint() {
        // Try to get complete solution from PuzzleGenerator
        val completePuzzle = PuzzleGenerator.getCompletePuzzle()

        if (completePuzzle == null) {
            Toast.makeText(this, "No hint available", Toast.LENGTH_SHORT).show()
            return
        }

        // Find an empty cell that should be filled
        val emptyCells = mutableListOf<Pair<Int, Int>>()

        for (i in 0 until size) {
            for (j in 0 until size) {
                // Skip initially visible cells
                if (initialVisibleCells.contains(Pair(i, j))) continue

                if (cells[i][j].text.toString().isEmpty()) {
                    emptyCells.add(Pair(i, j))
                }
            }
        }

        if (emptyCells.isEmpty()) {
            Toast.makeText(this, "All cells are filled already!", Toast.LENGTH_SHORT).show()
            return
        }

        // Select a random empty cell
        val randomCell = emptyCells.random()
        val i = randomCell.first
        val j = randomCell.second

        // Fill with correct symbol from complete puzzle
        if (i < completePuzzle.size && j < completePuzzle[i].size) {
            val correctSymbol = if (completePuzzle[i][j] == '☀') "\u2600" else "\u263E"
            cells[i][j].text = correctSymbol
            Toast.makeText(this, "Hint used: -10 points", Toast.LENGTH_SHORT).show()
            score -= 10
            if (score < 0) score = 0
            updateScoreAndStreakUI()
        } else {
            Toast.makeText(this, "No hint available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validatePuzzleRules(): Boolean {
        val sunSymbol = "\u2600" // ☀
        val moonSymbol = "\u263E" // ☾

        // Rule 1: Each cell must contain either sun or moon
        for (i in 0 until size) {
            for (j in 0 until size) {
                val cellContent = cells[i][j].text.toString()
                if (cellContent != sunSymbol && cellContent != moonSymbol) {
                    return false
                }
            }
        }

        // Rule 2: No more than 2 of the same type adjacent horizontally
        for (i in 0 until size) {
            for (j in 0 until size - 2) {
                val symbol1 = cells[i][j].text.toString()
                val symbol2 = cells[i][j+1].text.toString()
                val symbol3 = cells[i][j+2].text.toString()

                if (symbol1 == symbol2 && symbol2 == symbol3) {
                    return false
                }
            }
        }

        // Rule 3: No more than 2 of the same type adjacent vertically
        for (i in 0 until size - 2) {
            for (j in 0 until size) {
                val symbol1 = cells[i][j].text.toString()
                val symbol2 = cells[i+1][j].text.toString()
                val symbol3 = cells[i+2][j].text.toString()

                if (symbol1 == symbol2 && symbol2 == symbol3) {
                    return false
                }
            }
        }

        // Rule 4: Each row must have equal number of suns and moons
        for (i in 0 until size) {
            val rowSymbols = cells[i].map { it.text.toString() }
            val sunCount = rowSymbols.count { it == sunSymbol }
            val moonCount = rowSymbols.count { it == moonSymbol }

            if (sunCount != moonCount) {
                return false
            }
        }

        // Rule 5: Each column must have equal number of suns and moons
        for (j in 0 until size) {
            val colSymbols = cells.map { it[j].text.toString() }
            val sunCount = colSymbols.count { it == sunSymbol }
            val moonCount = colSymbols.count { it == moonSymbol }

            if (sunCount != moonCount) {
                return false
            }
        }

        // Rule 6: Cells separated by '=' must be the same type
        for (i in 0 until horizontalConstraints.size) {
            for (j in 0 until size) {
                if (j < horizontalConstraints[i].size && horizontalConstraints[i][j] == '=') {
                    val topCell = cells[i][j].text.toString()
                    val bottomCell = cells[i+1][j].text.toString()

                    if (topCell != bottomCell) {
                        return false
                    }
                }
            }
        }

        // Rule 7: Cells separated by 'x' must be different types
        for (i in 0 until horizontalConstraints.size) {
            for (j in 0 until size) {
                if (j < horizontalConstraints[i].size && horizontalConstraints[i][j] == 'x') {
                    val topCell = cells[i][j].text.toString()
                    val bottomCell = cells[i+1][j].text.toString()

                    if (topCell == bottomCell) {
                        return false
                    }
                }
            }
        }

        // Rule 8: Horizontal constraints for '=' and 'x'
        for (i in 0 until size) {
            for (j in 0 until verticalConstraints[0].size) {
                if (i < verticalConstraints.size && j < verticalConstraints[i].size) {
                    if (verticalConstraints[i][j] == '=') {
                        val leftCell = cells[i][j].text.toString()
                        val rightCell = cells[i][j+1].text.toString()

                        if (leftCell != rightCell) {
                            return false
                        }
                    } else if (verticalConstraints[i][j] == 'x') {
                        val leftCell = cells[i][j].text.toString()
                        val rightCell = cells[i][j+1].text.toString()

                        if (leftCell == rightCell) {
                            return false
                        }
                    }
                }
            }
        }

        return true
    }

    private fun showCompletionDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Puzzle Completed!")
        builder.setMessage("You've successfully completed today's Tango puzzle!\nYour score: $score\nYour streak: $streak")
        builder.setPositiveButton("OK") { _, _ ->
            // Maybe navigate back or offer to share score
        }
        builder.show()
    }

    private fun loadScoreAndStreak() {
        // Retrieve score and streak from shared preferences
        score = prefs.getInt("score", 0)
        streak = prefs.getInt("streak", 0)
    }

    private fun saveScoreAndStreak() {
        // Persist score and streak to shared preferences
        prefs.edit().putInt("score", score).putInt("streak", streak).apply()
    }

    private fun updateScoreAndStreakUI() {
        // Update the UI elements for score and streak
        scoreTextView.text = "Score: $score"
        streakTextView.text = "🔥 Streak: $streak"
    }
}