package com.example.iqfuse8

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class AptitudeTopicsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aptitude_topics)

        // Get references to each topic button
        val btnNumberSystem = findViewById<Button>(R.id.btn_number_system)
        val btnGcfLcm = findViewById<Button>(R.id.btn_gcf_lcm)
        val btnDecimalFractions = findViewById<Button>(R.id.btn_decimal_fractions)
        val btnSimplification = findViewById<Button>(R.id.btn_simplification)
        val btnSquareCubeRoots = findViewById<Button>(R.id.btn_square_cube_roots)
        val btnAverage = findViewById<Button>(R.id.btn_average)
        val btnProblemsOnNumbers = findViewById<Button>(R.id.btn_problems_on_numbers)
        val btnProblemsOnAges = findViewById<Button>(R.id.btn_problems_on_ages)
        val btnSurdsAndIndices = findViewById<Button>(R.id.btn_surs_and_indices)
        val btnLogarithms = findViewById<Button>(R.id.btn_logarithms)

        // Set click listeners for each button
        btnNumberSystem.setOnClickListener { launchQuestionsActivity("Number System") }
        btnGcfLcm.setOnClickListener { launchQuestionsActivity("G.C.F and L.C.M") }
        btnDecimalFractions.setOnClickListener { launchQuestionsActivity("Decimal Fractions") }
        btnSimplification.setOnClickListener { launchQuestionsActivity("Simplification") }
        btnSquareCubeRoots.setOnClickListener { launchQuestionsActivity("Square roots and cube roots") }
        btnAverage.setOnClickListener { launchQuestionsActivity("Average") }
        btnProblemsOnNumbers.setOnClickListener { launchQuestionsActivity("Problems on numbers") }
        btnProblemsOnAges.setOnClickListener { launchQuestionsActivity("Problems on Ages") }
        btnSurdsAndIndices.setOnClickListener { launchQuestionsActivity("Surds and Indices") }
        btnLogarithms.setOnClickListener { launchQuestionsActivity("Logarithms") }
    }

    private fun launchQuestionsActivity(topic: String) {
        val intent = Intent(this, QuestionsActivity::class.java)
        intent.putExtra("topic", topic)
        startActivity(intent)
    }
}
