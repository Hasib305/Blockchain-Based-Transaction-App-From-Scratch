package com.example.wisechoice

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.*

class SignInActivity : AppCompatActivity() {
    private lateinit var l_email: EditText
    private lateinit var l_pass: EditText
    private lateinit var databaseReference: DatabaseReference

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        l_email = findViewById(R.id.l_email)
        l_pass = findViewById(R.id.l_pass)
        databaseReference = FirebaseDatabase.getInstance().reference
    }

    fun signup(view: View) {
        startActivity(Intent(this, SignUpActivity::class.java))
    }

    fun updatePage(view: View) {
        val st_email = l_email.text.toString()
        val st_pass = l_pass.text.toString()

        if (st_email.isEmpty()) {
            l_email.error = "Please Enter The Email Address."
        } else if (st_pass.isEmpty()) {
            l_pass.error = "Please Enter The Password."
        } else {
            auth = Firebase.auth
            auth.signInWithEmailAndPassword(st_email, st_pass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        val user = auth.currentUser
                        val userID = user?.uid.toString()

                        val databaseReference = FirebaseDatabase.getInstance().getReference("users").child(userID)

                        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.exists()) {

                                    val st_phone = dataSnapshot.getValue(String::class.java)

                                    val sharedPrefs = getSharedPreferences(SignInActivity.PREFS_NAME, Context.MODE_PRIVATE)
                                    val editor = sharedPrefs.edit()
                                    editor.putBoolean("hasSignedIn", true)
                                    editor.putString("Account", st_phone.toString())
                                    editor.apply()

                                    val intent = Intent(this@SignInActivity, MinerTransactionActivity::class.java)
                                    startActivity(intent)
                                    finish()

                                } else {

                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {

                            }
                        })

                    } else {
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }

    companion object {
        const val PREFS_NAME = "MySharedPref"
    }

    fun forgotPassword(view: View) {
        val intent = Intent(this@SignInActivity, ForgotPasswordActivity::class.java)
        startActivity(intent)
    }
}