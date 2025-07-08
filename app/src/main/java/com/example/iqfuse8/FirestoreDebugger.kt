package com.example.iqfuse8

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

/**
 * Helper class to debug Firestore operations
 */
object FirestoreDebugger {
    private const val TAG = "FirestoreDebugger"
    
    /**
     * Log the contents of a Firestore query snapshot
     */
    fun logQuerySnapshot(snapshot: QuerySnapshot?, collectionName: String) {
        if (snapshot == null) {
            Log.e(TAG, "Query snapshot for $collectionName is null")
            return
        }
        
        if (snapshot.isEmpty) {
            Log.d(TAG, "Collection $collectionName is empty")
            return
        }
        
        Log.d(TAG, "Collection $collectionName has ${snapshot.size()} documents:")
        snapshot.documents.forEach { doc ->
            logDocumentSnapshot(doc, "$collectionName/${doc.id}")
        }
    }
    
    /**
     * Log the contents of a Firestore document snapshot
     */
    fun logDocumentSnapshot(snapshot: DocumentSnapshot?, documentPath: String) {
        if (snapshot == null) {
            Log.e(TAG, "Document snapshot for $documentPath is null")
            return
        }
        
        if (!snapshot.exists()) {
            Log.d(TAG, "Document $documentPath does not exist")
            return
        }
        
        val data = snapshot.data
        if (data.isNullOrEmpty()) {
            Log.d(TAG, "Document $documentPath exists but has no data")
            return
        }
        
        Log.d(TAG, "Document $documentPath data:")
        data.forEach { (key, value) ->
            val valueStr = when (value) {
                is Map<*, *> -> "Map with ${value.size} entries"
                is List<*> -> "List with ${value.size} items"
                else -> value.toString()
            }
            Log.d(TAG, "  $key: $valueStr")
        }
    }
    
    /**
     * Log the entire contents of Tango puzzles collection
     */
    fun logTangoPuzzles() {
        FirebaseManager.firestore.collection("tango_puzzles")
            .get()
            .addOnSuccessListener { snapshot ->
                Log.d(TAG, "===== TANGO PUZZLES =====")
                logQuerySnapshot(snapshot, "tango_puzzles")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting tango puzzles: ${e.message}", e)
            }
    }
} 