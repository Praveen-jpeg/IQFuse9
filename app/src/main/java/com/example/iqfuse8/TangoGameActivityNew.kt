package com.example.iqfuse8

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import java.util.Calendar

class TangoGameActivityNew : AppCompatActivity() {

    private lateinit var gridLayout: GridLayout
    private lateinit var checkButton: MaterialButton
    private lateinit var hintButton: MaterialButton
    private lateinit var undoButton: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var streakTextView: TextView
    private lateinit var scoreTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var newPuzzleButton: Button
    
    private val size = 6 // Grid size (6x6)
    private lateinit var puzzle: Array<CharArray> // Puzzle layout (partial)
    private lateinit var cells: Array<Array<TextView>> // Grid cells
    private lateinit var horizontalConstraints: Array<Array<Char>> // Horizontal constraints
    private lateinit var verticalConstraints: Array<Array<Char>> // Vertical constraints

    private var score = 0
    private var streak = 0
    private var secondsElapsed = 0
    private var timer: Timer? = null
    private val prefs by lazy { getSharedPreferences("TangoPrefs", MODE_PRIVATE) }
    private val auth: FirebaseAuth get() = FirebaseManager.auth
    private var isCompleted = false
    private var moveHistory = mutableListOf<Pair<Int, Int>>() // Track moves for undo
    private val TAG = "TangoGameActivityNew"
    
    // Characters for sun and moon
    private val SUN = '☀'
    private val MOON = '☾'
    private val EMPTY = ' '

    // List of initially visible cells
    private var initialVisibleCells = mutableListOf<Pair<Int, Int>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Log.d(TAG, "Starting onCreate")
            setContentView(R.layout.activity_tango_game_new)

            // Initialize Firebase components
            try {
                FirebaseManager.initialize(applicationContext)
                Log.d(TAG, "Firebase initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
                Toast.makeText(this, "Error initializing Firebase: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            // Initialize views
            try {
                initializeViews()
                Log.d(TAG, "Views initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing views: ${e.message}", e)
                Toast.makeText(this, "Error initializing views: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
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
            
            // Start timer
            startTimer()

            // Initialize Firebase auth and check authentication
            handleAuthentication()
            Log.d(TAG, "onCreate completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Fatal error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun initializeViews() {
        Log.d(TAG, "Initializing views")
        try {
            gridLayout = findViewById(R.id.gridLayout)
            checkButton = findViewById(R.id.btnCheckSolution)
            hintButton = findViewById(R.id.btnHint)
            undoButton = findViewById(R.id.btnUndo)
            backButton = findViewById(R.id.btnBack)
            streakTextView = findViewById(R.id.tvStreak)
            scoreTextView = findViewById(R.id.tvScore)
            timerTextView = findViewById(R.id.tvTimer)
            newPuzzleButton = findViewById(R.id.btnNewPuzzle)
        } catch (e: Exception) {
            Log.e(TAG, "Error finding views: ${e.message}", e)
            throw e
        }
    }

    private fun handleAuthentication() {
        Log.d(TAG, "Starting authentication")
        try {
            // For now, sign in anonymously if not already authenticated
            FirebaseManager.signInAnonymously { success ->
                if (success) {
                    Log.d(TAG, "Authentication successful")
                    // Check if user has completed today's puzzle
                    try {
                        checkCompletionStatus()
                        // Initialize puzzles and fetch today's puzzle
                        initializeAndFetchPuzzle()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error after authentication: ${e.message}", e)
                        runOnUiThread {
                            Toast.makeText(this, "Error after authentication: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Log.e(TAG, "Authentication failed")
                    runOnUiThread {
                        Toast.makeText(this, "Authentication failed, using local puzzle", Toast.LENGTH_LONG).show()
                    }
                    // Still try to fetch puzzle even if auth fails
                    try {
                        initializeAndFetchPuzzle()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error initializing puzzle after auth fail: ${e.message}", e)
                        runOnUiThread {
                            Toast.makeText(this, "Error initializing puzzle: ${e.message}", Toast.LENGTH_LONG).show()
                            // Initialize default empty puzzle as fallback
                            puzzle = Array(size) { CharArray(size) { ' ' } }
                            setupConstraints()
                            setupGrid()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in handleAuthentication: ${e.message}", e)
            // Initialize default empty puzzle as fallback
            puzzle = Array(size) { CharArray(size) { ' ' } }
            setupConstraints()
            setupGrid()
        }
    }

    private fun initializeAndFetchPuzzle() {
        Log.d(TAG, "Initializing puzzle")
        try {
            // Show loading indicator
            runOnUiThread {
                showSnackbar("Loading puzzle...")
            }
            
            // Create a default empty puzzle in case Firebase fails
            puzzle = Array(size) { CharArray(size) { ' ' } }
            
            FirebaseManager.forceUploadTangoPuzzles { success ->
                if (success) {
                    Log.d(TAG, "Successfully uploaded puzzles to Firestore")
                    
                    // Now fetch today's puzzle
                    runOnUiThread {
                        try {
                            fetchTodaysPuzzle()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching puzzle: ${e.message}", e)
                            showSnackbar("Error loading puzzle: ${e.message}")
                            setupConstraints()
                            setupGrid()
                        }
                    }
                } else {
                    Log.e(TAG, "Failed to upload puzzles to Firestore")
                    
                    // Use local puzzles as fallback
                    runOnUiThread {
                        try {
                            showSnackbar("Using local puzzle")
                            puzzle = PuzzleGenerator.getPartialPuzzleForDay(size)
                            setupConstraints()
                            setupGrid()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error setting up local puzzle: ${e.message}", e)
                            Toast.makeText(this, "Error setting up local puzzle: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in initializeAndFetchPuzzle: ${e.message}", e)
            // Use default empty grid as fallback
            runOnUiThread {
                Toast.makeText(this, "Error initializing puzzle: ${e.message}", Toast.LENGTH_LONG).show()
                setupConstraints()
                setupGrid()
            }
        }
    }

    private fun fetchTodaysPuzzle() {
        Log.d(TAG, "Fetching today's puzzle")
        try {
            // Show loading indicator
            showSnackbar("Loading puzzle...")
            
            // Use fixed puzzle (index 5) instead of daily puzzle
            PuzzleGenerator.getFixedPuzzle(5) { partialGrid, puzzleModel ->
                processPuzzleResult(partialGrid, puzzleModel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchTodaysPuzzle: ${e.message}", e)
            // Fallback to default
            puzzle = PuzzleGenerator.getPartialPuzzleForDay(size)
            setupConstraints()
            runOnUiThread {
                setupGrid()
                showSnackbar("Error loading puzzle, using default")
            }
        }
    }
    
    private fun loadRandomPuzzle() {
        Log.d(TAG, "Loading random puzzle")
        try {
            // Show loading indicator
            showSnackbar("Loading random puzzle...")
            
            // Get a random puzzle
            PuzzleGenerator.getRandomPuzzle { partialGrid, puzzleModel ->
                processPuzzleResult(partialGrid, puzzleModel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadRandomPuzzle: ${e.message}", e)
            // Fallback to default
            puzzle = PuzzleGenerator.getPartialPuzzleForDay(size)
            setupConstraints()
            runOnUiThread {
                setupGrid()
                showSnackbar("Error loading puzzle, using default")
            }
        }
    }
    
    private fun processPuzzleResult(partialGrid: Array<CharArray>?, puzzleModel: TangoPuzzleModel?) {
        try {
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
                    try {
                        setupGrid()
                        
                        // Confirm visibility to user
                        showSnackbar("Puzzle loaded!")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting up grid after fetch: ${e.message}", e)
                        Toast.makeText(this, "Error setting up grid: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Log.e(TAG, "Failed to fetch puzzle, using local puzzle")
                // Fallback to local puzzle
                puzzle = PuzzleGenerator.getPartialPuzzleForDay(size)
                setupConstraints() // Use default constraints
                runOnUiThread {
                    try {
                        setupGrid()
                        showSnackbar("Using local puzzle")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting up local grid: ${e.message}", e)
                        Toast.makeText(this, "Error setting up local grid: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing fetched puzzle: ${e.message}", e)
            // Fallback to default puzzle
            puzzle = PuzzleGenerator.getPartialPuzzleForDay(size)
            setupConstraints()
            runOnUiThread {
                try {
                    setupGrid()
                    showSnackbar("Error loading puzzle, using default")
                } catch (e2: Exception) {
                    Log.e(TAG, "Error setting up fallback grid: ${e2.message}", e2)
                    Toast.makeText(this, "Critical error setting up puzzle", Toast.LENGTH_LONG).show()
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
                        showSnackbar("You've already completed today's puzzle!")
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
        
        // Setup back button
        backButton.setOnClickListener {
            showExitConfirmationDialog()
        }
        
        // Setup new puzzle button
        newPuzzleButton.setOnClickListener {
            // Reset and load a new random puzzle
            resetGame()
            loadRandomPuzzle()
        }
    }
    
    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit Game")
            .setMessage("Are you sure you want to exit? Your progress will not be saved.")
            .setPositiveButton("Exit") { _, _ -> finish() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun checkSolution() {
        // First check using regular validation rules
        if (!validatePuzzleRules()) {
            showSnackbar("Your solution doesn't follow the game rules. Try again!")
            return
        }
        
        // Check if the puzzle is complete (no empty cells)
        if (!isPuzzleComplete()) {
            showSnackbar("Please fill in all empty cells before submitting")
            return
        }

        // Check that all pre-filled cells match the original puzzle
        for ((row, col) in initialVisibleCells) {
            if (getValueFromCell(row, col) != puzzle[row][col]) {
                showSnackbar("You changed a pre-filled cell!")
                return
            }
        }

        // If all checks pass, the solution is valid
        onPuzzleSolved()
    }
    
    private fun validatePuzzleRules(): Boolean {
        // Check for triple consecutive symbols in rows and columns
        if (!validateNoTripleSymbols()) return false
        
        // Check for equal number of suns and moons in each row and column
        if (!validateEqualDistribution()) return false
        
        // Constraint validation is now removed as requested.
        // Previously: if (!validateConstraints()) return false
        
        return true
    }
    
    private fun validateNoTripleSymbols(): Boolean {
        // Check rows for three consecutive same symbols
        for (row in 0 until size) {
            for (col in 0 until size - 2) {
                val c1 = getValueFromCell(row, col)
                val c2 = getValueFromCell(row, col + 1)
                val c3 = getValueFromCell(row, col + 2)
                
                // Skip if any cell is empty
                if (c1 == EMPTY || c2 == EMPTY || c3 == EMPTY) continue
                
                // Check if all three are the same
                if (c1 == c2 && c2 == c3) {
                    highlightInvalidCells(listOf(Pair(row, col), Pair(row, col + 1), Pair(row, col + 2)))
                    return false
                }
            }
        }
        
        // Check columns for three consecutive same symbols
        for (col in 0 until size) {
            for (row in 0 until size - 2) {
                val c1 = getValueFromCell(row, col)
                val c2 = getValueFromCell(row + 1, col)
                val c3 = getValueFromCell(row + 2, col)
                
                // Skip if any cell is empty
                if (c1 == EMPTY || c2 == EMPTY || c3 == EMPTY) continue
                
                // Check if all three are the same
                if (c1 == c2 && c2 == c3) {
                    highlightInvalidCells(listOf(Pair(row, col), Pair(row + 1, col), Pair(row + 2, col)))
                    return false
                }
            }
        }
        
        return true
    }
    
    private fun validateEqualDistribution(): Boolean {
        // For filled rows, check equal number of suns and moons
        for (row in 0 until size) {
            var sunCount = 0
            var moonCount = 0
            var hasEmpty = false
            
            for (col in 0 until size) {
                when (getValueFromCell(row, col)) {
                    SUN -> sunCount++
                    MOON -> moonCount++
                    EMPTY -> hasEmpty = true
                }
            }
            
            // Skip incomplete rows
            if (hasEmpty) continue
            
            // Check if suns and moons are balanced (for a 6x6 grid, each should have 3)
            if (sunCount != moonCount) {
                highlightInvalidRow(row)
                return false
            }
        }
        
        // For filled columns, check equal number of suns and moons
        for (col in 0 until size) {
            var sunCount = 0
            var moonCount = 0
            var hasEmpty = false
            
            for (row in 0 until size) {
                when (getValueFromCell(row, col)) {
                    SUN -> sunCount++
                    MOON -> moonCount++
                    EMPTY -> hasEmpty = true
                }
            }
            
            // Skip incomplete columns
            if (hasEmpty) continue
            
            // Check if suns and moons are balanced
            if (sunCount != moonCount) {
                highlightInvalidColumn(col)
                return false
            }
        }
        
        return true
    }
    
    private fun validateConstraints(): Boolean {
        // Check horizontal constraints
        for (row in 0 until size - 1) {
            for (col in 0 until size) {
                val constraint = horizontalConstraints[row][col]
                if (constraint != ' ') {
                    val upperCell = getValueFromCell(row, col)
                    val lowerCell = getValueFromCell(row + 1, col)
                    
                    // Skip if either cell is empty
                    if (upperCell == EMPTY || lowerCell == EMPTY) continue
                    
                    // Check constraint
                    when (constraint) {
                        '=' -> if (upperCell != lowerCell) {
                            highlightInvalidCells(listOf(Pair(row, col), Pair(row + 1, col)))
                            return false
                        }
                        'x' -> if (upperCell == lowerCell) {
                            highlightInvalidCells(listOf(Pair(row, col), Pair(row + 1, col)))
                            return false
                        }
                    }
                }
            }
        }
        
        // Check vertical constraints
        for (row in 0 until size) {
            for (col in 0 until size - 1) {
                val constraint = verticalConstraints[row][col]
                if (constraint != ' ') {
                    val leftCell = getValueFromCell(row, col)
                    val rightCell = getValueFromCell(row, col + 1)
                    
                    // Skip if either cell is empty
                    if (leftCell == EMPTY || rightCell == EMPTY) continue
                    
                    // Check constraint
                    when (constraint) {
                        '=' -> if (leftCell != rightCell) {
                            highlightInvalidCells(listOf(Pair(row, col), Pair(row, col + 1)))
                            return false
                        }
                        'x' -> if (leftCell == rightCell) {
                            highlightInvalidCells(listOf(Pair(row, col), Pair(row, col + 1)))
                            return false
                        }
                    }
                }
            }
        }
        
        return true
    }
    
    private fun highlightInvalidCells(cellList: List<Pair<Int, Int>>) {
        // Highlight invalid cells in red temporarily
        for ((row, col) in cellList) {
            cells[row][col].setBackgroundColor(Color.parseColor("#FFCDD2")) // Light red
        }
        
        // Reset after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            for ((row, col) in cellList) {
                // Reset to default color
                cells[row][col].setBackgroundColor(Color.WHITE)
            }
        }, 1500)
    }
    
    private fun highlightInvalidRow(row: Int) {
        for (col in 0 until size) {
            cells[row][col].setBackgroundColor(Color.parseColor("#FFCDD2")) // Light red
        }
        
        // Reset after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            for (col in 0 until size) {
                cells[row][col].setBackgroundColor(Color.WHITE)
            }
        }, 1500)
    }
    
    private fun highlightInvalidColumn(col: Int) {
        for (row in 0 until size) {
            cells[row][col].setBackgroundColor(Color.parseColor("#FFCDD2")) // Light red
        }
        
        // Reset after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            for (row in 0 until size) {
                cells[row][col].setBackgroundColor(Color.WHITE)
            }
        }, 1500)
    }
    
    private fun isPuzzleComplete(): Boolean {
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (getValueFromCell(row, col) == EMPTY) {
                    return false
                }
            }
        }
        return true
    }

    private fun onPuzzleSolved() {
        // Stop timer
        stopTimer()
        
        // Update statistics
        streak++
        score += calculateScore()
        
        // Save updated statistics
        saveScoreAndStreak()
        updateScoreAndStreakUI()
        
        // Mark as completed in Firebase and update leaderboard
        if (FirebaseManager.currentUser != null) {
            val userId = FirebaseManager.currentUser!!.uid
            val completionTime = secondsElapsed
            
            // Update user's completion status and time
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update(mapOf(
                    "tangoGameStreak" to streak,
                    "tangoGameScore" to score,
                    "lastPlayedTango" to System.currentTimeMillis(),
                    "lastTangoCompletionTime" to completionTime
                ))
                .addOnSuccessListener {
                    Log.d(TAG, "User stats updated successfully")
                    
                    // Update leaderboard
                    updateLeaderboard(userId, completionTime)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating user stats: ${e.message}", e)
                }
        }
        
        // Show congratulations dialog
        showCongratulationsDialog()
    }
    
    private fun updateLeaderboard(userId: String, completionTime: Int) {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val leaderboardDocId = "leaderboard_$dayOfMonth"
        
        // First fetch the user's display name from the users collection
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                // Get user's display name from Firestore or use a default
                val displayName = userDoc.getString("username") ?: "Anonymous Player"
                
                // Create leaderboard entry
                val leaderboardEntry = mapOf(
                    "userId" to userId,
                    "displayName" to displayName,
                    "completionTime" to completionTime,
                    "timestamp" to System.currentTimeMillis()
                )
                
                // Add to leaderboard collection
                FirebaseFirestore.getInstance().collection("tango_leaderboards")
                    .document(leaderboardDocId)
                    .collection("entries")
                    .document(userId)
                    .set(leaderboardEntry)
                    .addOnSuccessListener {
                        Log.d(TAG, "Leaderboard updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating leaderboard: ${e.message}", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching user data: ${e.message}", e)
                // If we can't get the user's name, use a default
                val leaderboardEntry = mapOf(
                    "userId" to userId,
                    "displayName" to "Anonymous Player",
                    "completionTime" to completionTime,
                    "timestamp" to System.currentTimeMillis()
                )
                
                // Add to leaderboard collection with default name
                FirebaseFirestore.getInstance().collection("tango_leaderboards")
                    .document(leaderboardDocId)
                    .collection("entries")
                    .document(userId)
                    .set(leaderboardEntry)
            }
    }
    
    private fun calculateScore(): Int {
        // Base score
        var newScore = 100
        
        // Time bonus - faster solutions get more points
        if (secondsElapsed < 60) {
            newScore += 50
        } else if (secondsElapsed < 120) {
            newScore += 30
        } else if (secondsElapsed < 180) {
            newScore += 20
        } else if (secondsElapsed < 300) {
            newScore += 10
        }
        
        // Streak bonus
        newScore += streak * 5
        
        return newScore
    }
    
    private fun showCongratulationsDialog() {
        try {
            Log.d(TAG, "Showing congratulations dialog")
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_congratulations, null)
            val scoreText = dialogView.findViewById<TextView>(R.id.tvFinalScore)
            val timeText = dialogView.findViewById<TextView>(R.id.tvFinalTime)
            val streakText = dialogView.findViewById<TextView>(R.id.tvStreak)
            val positionText = dialogView.findViewById<TextView>(R.id.tvPosition)
            
            scoreText.text = "Score: $score"
            timeText.text = "Time: ${formatTime(secondsElapsed)}"
            streakText.text = "🔥 Streak: $streak"
            positionText.text = "Loading rank..." // Default text while loading
            
            // Get leaderboard position
            if (FirebaseManager.currentUser != null) {
                val calendar = Calendar.getInstance()
                val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                val leaderboardDocId = "leaderboard_$dayOfMonth"
                
                FirebaseFirestore.getInstance().collection("tango_leaderboards")
                    .document(leaderboardDocId)
                    .collection("entries")
                    .orderBy("completionTime")
                    .get()
                    .addOnSuccessListener { documents ->
                        val position = documents.documents.indexOfFirst { it.id == FirebaseManager.currentUser!!.uid } + 1
                        val totalPlayers = documents.size()
                        
                        runOnUiThread {
                            positionText.text = "Rank: $position of $totalPlayers"
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error getting leaderboard position: ${e.message}", e)
                        runOnUiThread {
                            positionText.text = "Rank: --"
                        }
                    }
            } else {
                positionText.text = "Rank: --"
            }
            
            AlertDialog.Builder(this)
                .setTitle("Puzzle Solved!")
                .setView(dialogView)
                .setPositiveButton("New Puzzle") { _, _ ->
                    // Reset and load a new puzzle
                    resetGame()
                    fetchTodaysPuzzle()
                }
                .setNegativeButton("Share") { _, _ ->
                    // Implement sharing functionality
                    sharePuzzleResult()
                }
                .setNeutralButton("Exit") { _, _ ->
                    finish()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing congratulations dialog: ${e.message}", e)
            Toast.makeText(this, "Puzzle solved! Score: $score, Time: ${formatTime(secondsElapsed)}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun resetGame() {
        // Reset the puzzle state
        secondsElapsed = 0
        updateTimerDisplay()
        startTimer()
        moveHistory.clear()
    }
    
    private fun sharePuzzleResult() {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val leaderboardDocId = "leaderboard_$dayOfMonth"
        
        // Get leaderboard position first
        FirebaseFirestore.getInstance().collection("tango_leaderboards")
            .document(leaderboardDocId)
            .collection("entries")
            .orderBy("completionTime")
            .get()
            .addOnSuccessListener { documents ->
                val position = documents.documents.indexOfFirst { it.id == FirebaseManager.currentUser!!.uid } + 1
                val totalPlayers = documents.size()
                
                val shareMessage = "I solved today's Tango Puzzle in ${formatTime(secondsElapsed)} with a score of $score points! " +
                                 "Ranked #$position out of $totalPlayers players. " +
                                 "Current streak: $streak 🔥 #TangoPuzzle"
                
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
                    type = "text/plain"
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
            }
            .addOnFailureListener { e ->
                // If we can't get the rank, just share without it
                val shareMessage = "I solved today's Tango Puzzle in ${formatTime(secondsElapsed)} with a score of $score points! " +
                                 "Current streak: $streak 🔥 #TangoPuzzle"
                
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, shareMessage)
                    type = "text/plain"
                }
                startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
            }
    }

    private fun provideHint() {
        // Find all empty cells
        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until size) {
            for (col in 0 until size) {
                if (getValueFromCell(row, col) == EMPTY) {
                    emptyCells.add(Pair(row, col))
                }
            }
        }
        
        if (emptyCells.isEmpty()) {
            showSnackbar("No empty cells left!")
            return
        }
        
        // Select a random empty cell
        val hintCell = emptyCells.random()
        val (row, col) = hintCell
        
        // Set the correct value for the hint cell
        // In a real implementation, this would use the actual solution
        // For this implementation, we'll make a guess based on grid rules
        val hintValue = generateHintValue(row, col)
        cells[row][col].text = hintValue.toString()
        
        // Apply styling to show this was a hint
        cells[row][col].setBackgroundColor(Color.parseColor("#E8F5E9")) // Light green
        
        // Add to move history
        moveHistory.add(hintCell)
        
        // Score penalty for using a hint
        if (score >= 10) {
            score -= 10
            updateScoreAndStreakUI()
        }
        
        showSnackbar("Hint used! -10 points")
    }
    
    private fun generateHintValue(row: Int, col: Int): Char {
        // Count neighboring suns and moons
        var sunCount = 0
        var moonCount = 0
        
        // Check horizontal neighbors
        if (col > 0 && getValueFromCell(row, col - 1) == SUN) sunCount++
        if (col > 0 && getValueFromCell(row, col - 1) == MOON) moonCount++
        if (col < size - 1 && getValueFromCell(row, col + 1) == SUN) sunCount++
        if (col < size - 1 && getValueFromCell(row, col + 1) == MOON) moonCount++
        
        // Check vertical neighbors
        if (row > 0 && getValueFromCell(row - 1, col) == SUN) sunCount++
        if (row > 0 && getValueFromCell(row - 1, col) == MOON) moonCount++
        if (row < size - 1 && getValueFromCell(row + 1, col) == SUN) sunCount++
        if (row < size - 1 && getValueFromCell(row + 1, col) == MOON) moonCount++
        
        // Count suns and moons in the row and column
        var rowSuns = 0
        var rowMoons = 0
        var colSuns = 0
        var colMoons = 0
        
        for (i in 0 until size) {
            if (getValueFromCell(row, i) == SUN) rowSuns++
            if (getValueFromCell(row, i) == MOON) rowMoons++
            if (getValueFromCell(i, col) == SUN) colSuns++
            if (getValueFromCell(i, col) == MOON) colMoons++
        }
        
        // Check for triple prevention
        val needMoonForTriple = (col >= 2 && getValueFromCell(row, col - 1) == SUN && getValueFromCell(row, col - 2) == SUN) ||
                              (col <= size - 3 && getValueFromCell(row, col + 1) == SUN && getValueFromCell(row, col + 2) == SUN) ||
                              (col >= 1 && col <= size - 2 && getValueFromCell(row, col - 1) == SUN && getValueFromCell(row, col + 1) == SUN) ||
                              (row >= 2 && getValueFromCell(row - 1, col) == SUN && getValueFromCell(row - 2, col) == SUN) ||
                              (row <= size - 3 && getValueFromCell(row + 1, col) == SUN && getValueFromCell(row + 2, col) == SUN) ||
                              (row >= 1 && row <= size - 2 && getValueFromCell(row - 1, col) == SUN && getValueFromCell(row + 1, col) == SUN)
                              
        val needSunForTriple = (col >= 2 && getValueFromCell(row, col - 1) == MOON && getValueFromCell(row, col - 2) == MOON) ||
                               (col <= size - 3 && getValueFromCell(row, col + 1) == MOON && getValueFromCell(row, col + 2) == MOON) ||
                               (col >= 1 && col <= size - 2 && getValueFromCell(row, col - 1) == MOON && getValueFromCell(row, col + 1) == MOON) ||
                               (row >= 2 && getValueFromCell(row - 1, col) == MOON && getValueFromCell(row - 2, col) == MOON) ||
                               (row <= size - 3 && getValueFromCell(row + 1, col) == MOON && getValueFromCell(row + 2, col) == MOON) ||
                               (row >= 1 && row <= size - 2 && getValueFromCell(row - 1, col) == MOON && getValueFromCell(row + 1, col) == MOON)
        
        // Check constraints
        val constraintRequiresSun = checkConstraintRequirement(row, col, SUN)
        val constraintRequiresMoon = checkConstraintRequirement(row, col, MOON)
        
        // Decision logic - prioritize constraint requirements
        return when {
            constraintRequiresSun -> SUN
            constraintRequiresMoon -> MOON
            needSunForTriple -> SUN
            needMoonForTriple -> MOON
            rowSuns >= size/2 -> MOON
            rowMoons >= size/2 -> SUN
            colSuns >= size/2 -> MOON
            colMoons >= size/2 -> SUN
            sunCount > moonCount -> MOON
            moonCount > sunCount -> SUN
            else -> if (Math.random() < 0.5) SUN else MOON // Random if all else equal
        }
    }
    
    private fun checkConstraintRequirement(row: Int, col: Int, symbol: Char): Boolean {
        // Check horizontal constraints
        if (row > 0) {
            val upperConstraint = horizontalConstraints[row-1][col]
            if (upperConstraint == '=' && getValueFromCell(row-1, col) == symbol) return true
            if (upperConstraint == 'x' && getValueFromCell(row-1, col) != symbol && getValueFromCell(row-1, col) != EMPTY) return true
        }
        if (row < size - 1) {
            val lowerConstraint = horizontalConstraints[row][col]
            if (lowerConstraint == '=' && getValueFromCell(row+1, col) == symbol) return true
            if (lowerConstraint == 'x' && getValueFromCell(row+1, col) != symbol && getValueFromCell(row+1, col) != EMPTY) return true
        }
        
        // Check vertical constraints
        if (col > 0) {
            val leftConstraint = verticalConstraints[row][col-1]
            if (leftConstraint == '=' && getValueFromCell(row, col-1) == symbol) return true
            if (leftConstraint == 'x' && getValueFromCell(row, col-1) != symbol && getValueFromCell(row, col-1) != EMPTY) return true
        }
        if (col < size - 1) {
            val rightConstraint = verticalConstraints[row][col]
            if (rightConstraint == '=' && getValueFromCell(row, col+1) == symbol) return true
            if (rightConstraint == 'x' && getValueFromCell(row, col+1) != symbol && getValueFromCell(row, col+1) != EMPTY) return true
        }
        
        return false
    }

    private fun undoLastMove() {
        if (moveHistory.isEmpty()) {
            showSnackbar("No moves to undo")
            return
        }
        
        val (row, col) = moveHistory.removeAt(moveHistory.size - 1)
        
        // Check if this is a visible cell
        if (isInitiallyVisibleCell(row, col)) {
            showSnackbar("Cannot undo initially visible cells")
            return
        }
        
        // Reset cell to empty
        cells[row][col].text = ""
        cells[row][col].setBackgroundColor(Color.WHITE)
        
        showSnackbar("Move undone")
    }
    
    private fun isInitiallyVisibleCell(row: Int, col: Int): Boolean {
        return initialVisibleCells.contains(Pair(row, col))
    }

    private fun setupGrid() {
        Log.d(TAG, "Setting up grid")
        try {
            // Clear any existing views
            gridLayout.removeAllViews()
            
            // Calculate cell size based on screen width with better margins
            val displayMetrics = resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            
            // Account for more margins to ensure all columns fit
            val marginPixels = (48 * displayMetrics.density).toInt() // Increased margin to ensure all columns fit
            val gridWidth = screenWidth - marginPixels
            
            // Calculate cell size (divide grid width by number of columns)
            // Ensure we account for all cells plus spacing
            val cellSize = (gridWidth / size) - 4 // Subtract 4 pixels for cell margins
            
            // Set grid layout properties for better layout
            gridLayout.useDefaultMargins = false
            gridLayout.columnCount = size
            gridLayout.rowCount = size
            
            // Initialize grid
            for (row in 0 until size) {
                for (col in 0 until size) {
                    try {
                        // Inflate the cell layout
                        val cellView = LayoutInflater.from(this).inflate(R.layout.item_tango_cell_new, null) as CardView
                        val cellText = cellView.findViewById<TextView>(R.id.cellText)
                        
                        // Set cell size
                        val params = GridLayout.LayoutParams()
                        params.width = cellSize
                        params.height = cellSize
                        params.setMargins(2)
                        params.rowSpec = GridLayout.spec(row)
                        params.columnSpec = GridLayout.spec(col)
                        
                        // Set initial value if this is a visible cell
                        if (row < puzzle.size && col < puzzle[row].size && puzzle[row][col] != ' ') {
                            cellText.text = puzzle[row][col].toString()
                            
                            // If this is a fixed cell, use a different background color
                            if (initialVisibleCells.contains(Pair(row, col))) {
                                cellText.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue
                            }
                        } else {
                            // Allow user to tap on empty cells to toggle between sun and moon
                            cellView.setOnClickListener {
                                // Skip if the game is completed
                                if (isCompleted) {
                                    showSnackbar("Puzzle already completed!")
                                    return@setOnClickListener
                                }
                                
                                // Skip if this is an initially visible cell
                                if (isInitiallyVisibleCell(row, col)) {
                                    showSnackbar("Cannot modify given cells")
                                    return@setOnClickListener
                                }
                                
                                // Toggle cell value
                                toggleCellValue(cellText, row, col)
                            }
                        }
                        
                        // Add cell to grid
                        gridLayout.addView(cellView, params)
                        
                        // Store reference to the cell text view
                        cells[row][col] = cellText
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting up cell [$row][$col]: ${e.message}", e)
                    }
                }
            }
            
            // Using a simpler approach for constraints - add them directly to cell borders instead of as separate views
            addSimplifiedConstraints()
        } catch (e: Exception) {
            Log.e(TAG, "Error in setupGrid: ${e.message}", e)
            Toast.makeText(this, "Error setting up game grid: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun toggleCellValue(cellText: TextView, row: Int, col: Int) {
        // Toggle between empty, sun, and moon
        when (cellText.text.toString()) {
            "" -> {
                cellText.text = SUN.toString()
                moveHistory.add(Pair(row, col))
            }
            SUN.toString() -> {
                cellText.text = MOON.toString()
                // Don't add to history - just modifying the same cell
            }
            else -> {
                cellText.text = ""
                // Remove from history since we're back to empty
                if (moveHistory.contains(Pair(row, col))) {
                    moveHistory.remove(Pair(row, col))
                }
            }
        }
    }
    
    private fun addSimplifiedConstraints() {
        try {
            // Instead of adding constraint markers as separate views, we'll highlight cell borders
            // This is a simplified approach to avoid layout issues
            
            // Process horizontal constraints
            for (row in 0 until size - 1) {
                for (col in 0 until size) {
                    val constraint = horizontalConstraints[row][col]
                    if (constraint != ' ') {
                        // Apply visual indicator to the cells above and below
                        // For this simplified version, we'll just add a background color indicator
                        val upperCell = cells[row][col]
                        val lowerCell = cells[row + 1][col]
                        
                        when (constraint) {
                            '=' -> {
                                // Equal constraint (=) - blue border
                                upperCell.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue
                                lowerCell.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue
                            }
                            'x' -> {
                                // Different constraint (x) - light red border
                                upperCell.setBackgroundColor(Color.parseColor("#FFEBEE")) // Light red
                                lowerCell.setBackgroundColor(Color.parseColor("#FFEBEE")) // Light red
                            }
                        }
                    }
                }
            }
            
            // Process vertical constraints
            for (row in 0 until size) {
                for (col in 0 until size - 1) {
                    val constraint = verticalConstraints[row][col]
                    if (constraint != ' ') {
                        // Apply visual indicator to the cells left and right
                        val leftCell = cells[row][col]
                        val rightCell = cells[row][col + 1]
                        
                        when (constraint) {
                            '=' -> {
                                // Equal constraint (=) - light blue border
                                leftCell.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue
                                rightCell.setBackgroundColor(Color.parseColor("#E3F2FD")) // Light blue
                            }
                            'x' -> {
                                // Different constraint (x) - light red border
                                leftCell.setBackgroundColor(Color.parseColor("#FFEBEE")) // Light red
                                rightCell.setBackgroundColor(Color.parseColor("#FFEBEE")) // Light red
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in addSimplifiedConstraints: ${e.message}", e)
        }
    }
    
    private fun setupConstraints() {
        // Default constraints if needed
        // In this implementation, we're mostly relying on fetched constraints
        
        // Create some simple constraints for local puzzles
        for (i in 0 until size - 1) {
            for (j in 0 until size) {
                horizontalConstraints[i][j] = ' '
            }
        }
        
        for (i in 0 until size) {
            for (j in 0 until size - 1) {
                verticalConstraints[i][j] = ' '
            }
        }
        
        // Add a few sample constraints
        horizontalConstraints[1][2] = '='
        horizontalConstraints[3][4] = 'x'
        verticalConstraints[2][1] = 'x'
        verticalConstraints[4][3] = '='
    }

    private fun getValueFromCell(row: Int, col: Int): Char {
        if (cells[row][col].text.isEmpty()) return EMPTY
        return cells[row][col].text[0]
    }
    
    private fun loadScoreAndStreak() {
        score = prefs.getInt("score", 0)
        streak = prefs.getInt("streak", 0)
    }
    
    private fun saveScoreAndStreak() {
        // Save locally
        prefs.edit().apply {
            putInt("score", score)
            putInt("streak", streak)
            apply()
        }
        
        // Save to Firestore for dashboard display
        FirebaseManager.currentUser?.uid?.let { userId ->
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update(mapOf(
                    "tangoGameStreak" to streak,
                    "tangoGameScore" to score,
                    "lastPlayedTango" to System.currentTimeMillis()
                ))
                .addOnSuccessListener {
                    Log.d(TAG, "Tango game streak updated in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating Tango game streak in Firestore: ${e.message}", e)
                }
        }
    }
    
    private fun updateScoreAndStreakUI() {
        streakTextView.text = "🔥 $streak"
        scoreTextView.text = "$score"
    }
    
    private fun startTimer() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                secondsElapsed++
                runOnUiThread {
                    updateTimerDisplay()
                }
            }
        }, 1000, 1000)
    }
    
    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }
    
    private fun updateTimerDisplay() {
        timerTextView.text = formatTime(secondsElapsed)
    }
    
    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(gridLayout, message, Snackbar.LENGTH_SHORT).show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopTimer()
    }
    
    override fun onPause() {
        super.onPause()
        // Pause timer when app goes to background
        stopTimer()
    }
    
    override fun onResume() {
        super.onResume()
        // Resume timer if game is active
        if (timer == null && !isCompleted) {
            startTimer()
        }
    }
} 