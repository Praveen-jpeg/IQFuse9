package com.example.iqfuse8

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Centralized manager for all Firebase related operations.
 * Ensures Firebase is properly initialized across the app.
 */
object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    // Firestore instance
    private var _firestore: FirebaseFirestore? = null
    val firestore: FirebaseFirestore
        get() {
            if (_firestore == null) {
                _firestore = Firebase.firestore
            }
            return _firestore!!
        }
    
    // Auth instance
    private var _auth: FirebaseAuth? = null
    val auth: FirebaseAuth
        get() {
            if (_auth == null) {
                _auth = Firebase.auth
            }
            return _auth!!
        }
    
    // Current user
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    // Initialization flag
    private var isInitialized = false
    
    /**
     * Initialize Firebase components
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(context)
            
            // Get instances to ensure initialization
            _firestore = Firebase.firestore
            _auth = Firebase.auth
            
            // Set offline persistence for Firestore
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            _firestore?.firestoreSettings = settings
            
            // Mark as initialized
            isInitialized = true
            Log.d(TAG, "Firebase successfully initialized")
            
            // If not signed in, sign in anonymously
            if (_auth?.currentUser == null) {
                signInAnonymously { success ->
                    Log.d(TAG, "Anonymous auth result: $success")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
        }
    }
    
    /**
     * Sign in anonymously if no user is signed in
     */
    fun signInAnonymously(callback: (Boolean) -> Unit) {
        if (auth.currentUser != null) {
            callback(true)
            return
        }
        
        auth.signInAnonymously()
            .addOnSuccessListener {
                Log.d(TAG, "Anonymous authentication successful")
                callback(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Anonymous authentication failed: ${e.message}", e)
                callback(false)
            }
    }
    
    /**
     * Initialize Tango puzzle data in Firestore
     */
    fun initializeTangoPuzzles() {
        PuzzleGenerator.initializeFirestorePuzzles()
    }
    
    /**
     * Force upload of all Tango puzzles to ensure they're in Firestore
     */
    fun forceUploadTangoPuzzles(callback: (Boolean) -> Unit) {
        Log.d(TAG, "Force uploading all Tango puzzles to Firestore")
        val db = firestore
        val collection = "tango_puzzles"
        
        // Create a batch to upload all puzzles at once
        val batch = db.batch()
        
        // Generate 10 puzzles
        for (i in 0 until 10) {
            val puzzleModel = TangoPuzzleModel.createSamplePuzzle(i)
            val docRef = db.collection(collection).document("puzzle_$i")
            
            // Add puzzle to batch
            batch.set(docRef, puzzleModel)
            
            // Log details for debugging
            Log.d(TAG, "Added puzzle_$i to batch: ${puzzleModel.grid.size} rows, " +
                    "${puzzleModel.visibleCells.size} visible cells")
        }
        
        // Commit the batch
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "Successfully uploaded all puzzles to Firestore")
                
                // Verify puzzles were uploaded
                db.collection(collection).get()
                    .addOnSuccessListener { documents ->
                        Log.d(TAG, "Found ${documents.size()} puzzles in Firestore")
                        documents.forEach { doc ->
                            Log.d(TAG, "Puzzle ${doc.id} exists in Firestore")
                        }
                        callback(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error verifying puzzles: ${e.message}", e)
                        callback(false)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading puzzles: ${e.message}", e)
                callback(false)
            }
    }
} 