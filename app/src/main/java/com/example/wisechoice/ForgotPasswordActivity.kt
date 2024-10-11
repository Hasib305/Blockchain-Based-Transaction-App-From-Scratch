package com.example.wisechoice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var forgotButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = Firebase.auth

        emailEditText = findViewById(R.id.email)
        forgotButton = findViewById(R.id.submit)

        forgotButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }

            else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Failed to send reset email. Please check your email and try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }

}