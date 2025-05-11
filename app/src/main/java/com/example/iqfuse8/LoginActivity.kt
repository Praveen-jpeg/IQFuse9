package com.example.iqfuse8

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.iqfuse8.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        val currentUser = FirebaseAuth.getInstance().currentUser

        // If user is logged in and session is valid
        if (isLoggedIn && currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (!isFinishing && !isDestroyed) {
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                                // Save login state
                                sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()

                                val intent = Intent(this, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                val error = task.exception?.message ?: "Login failed"
                                Toast.makeText(this, "Login Failed: $error", Toast.LENGTH_LONG).show()
                                Log.e("LoginActivity", "Firebase login error", task.exception)
                            }
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.signupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }
    }
}
