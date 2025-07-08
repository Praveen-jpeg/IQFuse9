package com.example.iqfuse8

import android.util.Log
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

object PuzzleGenerator {
    private const val TAG = "PuzzleGenerator"
    private val db: FirebaseFirestore get() = FirebaseManager.firestore
    private val COLLECTION_NAME = "tango_puzzles"
    private var initializedPuzzles = false
    private const val DEFAULT_PUZZLE_SIZE = 6

    // Store the complete puzzle solution for validation
    private var currentCompletePuzzle: Array<CharArray>? = null

    // Initialize Firestore with the default puzzles if not already there
    fun initializeFirestorePuzzles() {
        if (initializedPuzzles) return

        Log.d(TAG, "Checking if puzzles need to be initialized")

        // Check if puzzles collection exists
        db.collection(COLLECTION_NAME).get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "No puzzles found, initializing default puzzles")
                    // If no puzzles exist, add 10 sample puzzles
                    for (i in 0 until 10) {
                        val puzzle = TangoPuzzleModel.createSamplePuzzle(i)
                        db.collection(COLLECTION_NAME).document(puzzle.id)
                            .set(puzzle)
                            .addOnSuccessListener {
                                Log.d(TAG, "Successfully saved puzzle ${puzzle.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to save puzzle ${puzzle.id}: ${e.message}", e)
                            }
                    }
                } else {
                    Log.d(TAG, "Puzzles already exist in Firestore: ${documents.size()}")
                    for (doc in documents) {
                        Log.d(TAG, "Puzzle found: ${doc.id}")
                    }
                }
                initializedPuzzles = true
            }
            .addOnFailureListener { e ->
                // If failed to check, still mark as initialized to avoid repeated attempts
                Log.e(TAG, "Failed to check puzzles: ${e.message}", e)
                initializedPuzzles = true
            }
    }

    // Default puzzle to use as fallback
    private fun getDefaultPuzzle(dayIndex: Int): TangoPuzzleModel {
        return TangoPuzzleModel.createSamplePuzzle(dayIndex)
    }

    // Get partial puzzle for display
    fun getPartialPuzzleForDay(size: Int = DEFAULT_PUZZLE_SIZE): Array<CharArray> {
        // Create empty grid
        val grid = Array(size) { CharArray(size) { ' ' } }
        
        // Generate 16 random visible cells based on day seed
        val visibleCells = mutableSetOf<Pair<Int, Int>>()
        val random = java.util.Random(Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toLong())
        
        while (visibleCells.size < 16) {
            val row = random.nextInt(size)
            val col = random.nextInt(size)
            val cell = Pair(row, col)
            
            if (!visibleCells.contains(cell)) {
                visibleCells.add(cell)
                // Set initial value (sun or moon)
                grid[row][col] = if (random.nextBoolean()) '☀' else '☾'
            }
        }
        
        return grid
    }

    // Get complete puzzle for validation
    fun getCompletePuzzle(): Array<CharArray>? {
        return currentCompletePuzzle
    }

    // Get puzzle with constraints from Firestore with callback
    fun getPuzzleFromFirestore(callback: (partialGrid: Array<CharArray>?, model: TangoPuzzleModel?) -> Unit) {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val dayIndex = (dayOfMonth - 1) % 10  // Use mod 10 to cycle through 10 puzzles

        Log.d(TAG, "Fetching puzzle for day $dayOfMonth (index $dayIndex) from Firestore")

        // Ensure we're using the Firebase Manager's Firestore instance
        val firestore = FirebaseManager.firestore

        firestore.collection(COLLECTION_NAME).document("puzzle_$dayIndex")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        Log.d(TAG, "Puzzle document exists: ${document.id}")
                        val puzzleModel = document.toObject(TangoPuzzleModel::class.java)
                        if (puzzleModel != null) {
                            Log.d(TAG, "Successfully converted to TangoPuzzleModel")

                            // Debug log the grid data
                            puzzleModel.grid.forEachIndexed { index, row ->
                                Log.d(TAG, "Grid row $index: '$row' (length: ${row.length})")
                            }

                            // Create complete grid
                            val completeGrid = TangoPuzzleModel.gridToCharArray(puzzleModel.grid)

                            // Store it for validation
                            currentCompletePuzzle = completeGrid

                            // Create partial grid showing only visible cells
                            val partialGrid = TangoPuzzleModel.createPartialGrid(
                                completeGrid,
                                puzzleModel.visibleCells
                            )

                            callback(partialGrid, puzzleModel)
                        } else {
                            Log.e(TAG, "Failed to convert document to TangoPuzzleModel")
                            val defaultPuzzle = getDefaultPuzzle(dayIndex)
                            val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                            currentCompletePuzzle = completeGrid
                            val partialGrid = TangoPuzzleModel.createPartialGrid(
                                completeGrid,
                                defaultPuzzle.visibleCells
                            )
                            callback(partialGrid, defaultPuzzle)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception parsing puzzle: ${e.message}", e)
                        val defaultPuzzle = getDefaultPuzzle(dayIndex)
                        val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                        currentCompletePuzzle = completeGrid
                        val partialGrid = TangoPuzzleModel.createPartialGrid(
                            completeGrid,
                            defaultPuzzle.visibleCells
                        )
                        callback(partialGrid, defaultPuzzle)
                    }
                } else {
                    Log.d(TAG, "Puzzle document doesn't exist, using default")
                    val defaultPuzzle = getDefaultPuzzle(dayIndex)
                    val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                    currentCompletePuzzle = completeGrid
                    val partialGrid = TangoPuzzleModel.createPartialGrid(
                        completeGrid,
                        defaultPuzzle.visibleCells
                    )
                    callback(partialGrid, defaultPuzzle)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching puzzle: ${e.message}", e)
                val defaultPuzzle = getDefaultPuzzle(dayIndex)
                val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                currentCompletePuzzle = completeGrid
                val partialGrid = TangoPuzzleModel.createPartialGrid(
                    completeGrid,
                    defaultPuzzle.visibleCells
                )
                callback(partialGrid, defaultPuzzle)
            }
    }

    // Validate user's solution against the complete puzzle
    fun validateUserSolution(userGrid: Array<Array<TextView>>): Boolean {
        if (currentCompletePuzzle == null) {
            Log.e(TAG, "No complete puzzle available for validation")
            return false
        }

        // Check if user's solution matches the complete puzzle
        for (i in userGrid.indices) {
            for (j in userGrid[i].indices) {
                if (i < currentCompletePuzzle!!.size && j < currentCompletePuzzle!![i].size) {
                    val userCell = userGrid[i][j].text.toString()
                    val expectedCell = currentCompletePuzzle!![i][j].toString()

                    // If sun or moon, directly compare
                    if (userCell == "☀" && expectedCell != "☀") return false
                    if (userCell == "☾" && expectedCell != "☾") return false

                    // If empty, that's incorrect (all cells should be filled)
                    if (userCell.isEmpty()) return false
                }
            }
        }

        return true
    }

    // Mark a puzzle as completed for a user
    fun markPuzzleAsCompleted(userId: String) {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val completionRecord = hashMapOf(
            "userId" to userId,
            "dayOfMonth" to dayOfMonth,
            "month" to month,
            "year" to year,
            "timestamp" to Calendar.getInstance().timeInMillis
        )

        Log.d(TAG, "Marking puzzle as completed for user $userId")

        db.collection("puzzle_completions")
            .add(completionRecord)
            .addOnSuccessListener { docRef ->
                Log.d(TAG, "Puzzle completion saved with ID: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving puzzle completion: ${e.message}", e)
            }
    }

    // Check if user has completed today's puzzle
    fun checkPuzzleCompletionStatus(userId: String, callback: (Boolean) -> Unit) {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        Log.d(TAG, "Checking if user $userId has completed today's puzzle")

        db.collection("puzzle_completions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("dayOfMonth", dayOfMonth)
            .whereEqualTo("month", month)
            .whereEqualTo("year", year)
            .get()
            .addOnSuccessListener { documents ->
                val completed = documents.size() > 0
                Log.d(TAG, "User completion status: $completed")
                callback(completed)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking completion status: ${e.message}", e)
                callback(false)
            }
    }
    
    // Get a random puzzle regardless of the day
    fun getRandomPuzzle(callback: (partialGrid: Array<CharArray>?, model: TangoPuzzleModel?) -> Unit) {
        // Generate a random index between 0-9
        val randomIndex = (0..9).random()
        
        Log.d(TAG, "Fetching random puzzle with index $randomIndex from Firestore")
        
        // Ensure we're using the Firebase Manager's Firestore instance
        val firestore = FirebaseManager.firestore
        
        firestore.collection(COLLECTION_NAME).document("puzzle_$randomIndex")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        Log.d(TAG, "Puzzle document exists: ${document.id}")
                        val puzzleModel = document.toObject(TangoPuzzleModel::class.java)
                        if (puzzleModel != null) {
                            Log.d(TAG, "Successfully converted to TangoPuzzleModel")
                            
                            // Create complete grid
                            val completeGrid = TangoPuzzleModel.gridToCharArray(puzzleModel.grid)
                            
                            // Store it for validation
                            currentCompletePuzzle = completeGrid
                            
                            // Create partial grid showing only visible cells
                            val partialGrid = TangoPuzzleModel.createPartialGrid(
                                completeGrid,
                                puzzleModel.visibleCells
                            )
                            
                            callback(partialGrid, puzzleModel)
                        } else {
                            Log.e(TAG, "Failed to convert document to TangoPuzzleModel")
                            val defaultPuzzle = getDefaultPuzzle(randomIndex)
                            val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                            currentCompletePuzzle = completeGrid
                            val partialGrid = TangoPuzzleModel.createPartialGrid(
                                completeGrid,
                                defaultPuzzle.visibleCells
                            )
                            callback(partialGrid, defaultPuzzle)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception parsing puzzle: ${e.message}", e)
                        val defaultPuzzle = getDefaultPuzzle(randomIndex)
                        val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                        currentCompletePuzzle = completeGrid
                        val partialGrid = TangoPuzzleModel.createPartialGrid(
                            completeGrid,
                            defaultPuzzle.visibleCells
                        )
                        callback(partialGrid, defaultPuzzle)
                    }
                } else {
                    Log.d(TAG, "Puzzle document doesn't exist, using default")
                    val defaultPuzzle = getDefaultPuzzle(randomIndex)
                    val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                    currentCompletePuzzle = completeGrid
                    val partialGrid = TangoPuzzleModel.createPartialGrid(
                        completeGrid,
                        defaultPuzzle.visibleCells
                    )
                    callback(partialGrid, defaultPuzzle)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching puzzle: ${e.message}", e)
                val defaultPuzzle = getDefaultPuzzle(randomIndex)
                val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                currentCompletePuzzle = completeGrid
                val partialGrid = TangoPuzzleModel.createPartialGrid(
                    completeGrid,
                    defaultPuzzle.visibleCells
                )
                callback(partialGrid, defaultPuzzle)
            }
    }
    
    // Get a fixed puzzle regardless of the day (useful for testing or specific puzzles)
    fun getFixedPuzzle(puzzleIndex: Int = 5, callback: (partialGrid: Array<CharArray>?, model: TangoPuzzleModel?) -> Unit) {
        Log.d(TAG, "Fetching fixed puzzle with index $puzzleIndex from Firestore")
        
        // Ensure we're using the Firebase Manager's Firestore instance
        val firestore = FirebaseManager.firestore
        
        firestore.collection(COLLECTION_NAME).document("puzzle_$puzzleIndex")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        Log.d(TAG, "Fixed puzzle document exists: ${document.id}")
                        val puzzleModel = document.toObject(TangoPuzzleModel::class.java)
                        if (puzzleModel != null) {
                            Log.d(TAG, "Successfully converted to TangoPuzzleModel")
                            
                            // Create complete grid
                            val completeGrid = TangoPuzzleModel.gridToCharArray(puzzleModel.grid)
                            
                            // Store it for validation
                            currentCompletePuzzle = completeGrid
                            
                            // Create partial grid showing only visible cells
                            val partialGrid = TangoPuzzleModel.createPartialGrid(
                                completeGrid,
                                puzzleModel.visibleCells
                            )
                            
                            callback(partialGrid, puzzleModel)
                        } else {
                            Log.e(TAG, "Failed to convert document to TangoPuzzleModel")
                            val defaultPuzzle = getDefaultPuzzle(puzzleIndex)
                            val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                            currentCompletePuzzle = completeGrid
                            val partialGrid = TangoPuzzleModel.createPartialGrid(
                                completeGrid,
                                defaultPuzzle.visibleCells
                            )
                            callback(partialGrid, defaultPuzzle)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception parsing puzzle: ${e.message}", e)
                        val defaultPuzzle = getDefaultPuzzle(puzzleIndex)
                        val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                        currentCompletePuzzle = completeGrid
                        val partialGrid = TangoPuzzleModel.createPartialGrid(
                            completeGrid,
                            defaultPuzzle.visibleCells
                        )
                        callback(partialGrid, defaultPuzzle)
                    }
                } else {
                    Log.d(TAG, "Puzzle document doesn't exist, using default")
                    val defaultPuzzle = getDefaultPuzzle(puzzleIndex)
                    val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                    currentCompletePuzzle = completeGrid
                    val partialGrid = TangoPuzzleModel.createPartialGrid(
                        completeGrid,
                        defaultPuzzle.visibleCells
                    )
                    callback(partialGrid, defaultPuzzle)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching puzzle: ${e.message}", e)
                val defaultPuzzle = getDefaultPuzzle(puzzleIndex)
                val completeGrid = TangoPuzzleModel.gridToCharArray(defaultPuzzle.grid)
                currentCompletePuzzle = completeGrid
                val partialGrid = TangoPuzzleModel.createPartialGrid(
                    completeGrid,
                    defaultPuzzle.visibleCells
                )
                callback(partialGrid, defaultPuzzle)
            }
    }
}
