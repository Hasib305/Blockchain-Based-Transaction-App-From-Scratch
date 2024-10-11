package com.example.wisechoice

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

class SignUpActivity : AppCompatActivity() {

    private lateinit var r_name: EditText
    private lateinit var r_email: EditText
    private lateinit var r_pass: EditText
    private lateinit var r_signup: Button
    private lateinit var timeText: TextView
    private lateinit var databaseReference: DatabaseReference
    private lateinit var r_privateKey: String

    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        r_name = findViewById(R.id.r_name)
        r_email = findViewById(R.id.r_email)
        r_pass = findViewById(R.id.r_pass)
        r_signup = findViewById(R.id.bur_signup)
        timeText = findViewById(R.id.time_text)
        databaseReference = FirebaseDatabase.getInstance().reference

        r_signup.setOnClickListener(View.OnClickListener {

            val startTime = System.currentTimeMillis()

            val st_name = r_name.text.toString()
            val st_email = r_email.text.toString()
            val st_pass = r_pass.text.toString()

            if (st_name.isEmpty()) {
                r_name.setError("User Name Is Required.")
                Toast.makeText(this@SignUpActivity, "User Name Is Required.",
                    Toast.LENGTH_SHORT).show()
            }
            else if (st_email.isEmpty()) {
                    r_email.setError("Email Is Required.")
                    Toast.makeText(this@SignUpActivity, "Email Is Required.",
                        Toast.LENGTH_SHORT).show()
            } else if (st_pass.isEmpty()) {
                r_pass.setError("Password Is Required.")
                Toast.makeText(this@SignUpActivity, "Password Is Required.",
                    Toast.LENGTH_SHORT).show()
            }
            else if (st_pass.length < 6) {
                r_pass.setError("Password Should Be Of At Least 6 Characters.")
                Toast.makeText(this@SignUpActivity, "Password Should Be Of At Least 6 Characters.",
                    Toast.LENGTH_SHORT).show()
            }
            else {
                auth = Firebase.auth

                fun generateUniqueNumber(): Int {
                    return (10000..99999).random()
                }

                fun isNumberInUse(number: Int, onComplete: (Boolean) -> Unit) {
                    val usersRef = databaseReference.child("users")

                    usersRef.orderByValue().equalTo(number.toDouble())
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                onComplete(snapshot.exists())
                            }

                            override fun onCancelled(error: DatabaseError) {
                                onComplete(false)
                            }
                        })
                }

                val uniqueNumber = generateUniqueNumber()

                isNumberInUse(uniqueNumber) { isUsed ->
                    if (!isUsed) {
                        auth.createUserWithEmailAndPassword(st_email, st_pass)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    val userID = user?.uid.toString()

                                    var st_phone = uniqueNumber.toString()

                                    databaseReference.child("users").child(userID)
                                        .setValue(st_phone)

                                    val keyPair = generateKeyPair()

                                    val publicKey = keyPair.public
                                    val privateKey = keyPair.private

                                    r_privateKey =
                                        Base64.getEncoder().encodeToString(privateKey.encoded)

                                    databaseReference.child("miners")
                                        .addListenerForSingleValueEvent(object :
                                            ValueEventListener {
                                            override fun onDataChange(snapshot: DataSnapshot) {
                                                val phoneNumbers =
                                                    snapshot.children.mapNotNull { it.key }

                                                if (phoneNumbers.isEmpty()) {
                                                    saveNewUser(
                                                        st_name, st_phone,
                                                        Base64.getEncoder()
                                                            .encodeToString(publicKey.encoded)
                                                    )

                                                } else if (phoneNumbers.size >= 1) {
                                                    val random1 = phoneNumbers.random()
                                                    val random2 = phoneNumbers.random()
                                                    val random3 = phoneNumbers.random()
                                                    val random4 = phoneNumbers.random()
                                                    val random5 = phoneNumbers.random()

                                                    val balance1 = snapshot.child(random1)
                                                        .child("Users_Balance")
                                                    val balance2 = snapshot.child(random2)
                                                        .child("Users_Balance")
                                                    val balance3 = snapshot.child(random3)
                                                        .child("Users_Balance")
                                                    val balance4 = snapshot.child(random4)
                                                        .child("Users_Balance")
                                                    val balance5 = snapshot.child(random5)
                                                        .child("Users_Balance")

                                                    val blockchain1 = snapshot.child(random1)
                                                        .child("main_blockchain")
                                                    val blockchain2 = snapshot.child(random2)
                                                        .child("main_blockchain")
                                                    val blockchain3 = snapshot.child(random3)
                                                        .child("main_blockchain")
                                                    val blockchain4 = snapshot.child(random3)
                                                        .child("main_blockchain")
                                                    val blockchain5 = snapshot.child(random5)
                                                        .child("main_blockchain")

                                                    if (balance1.exists() && balance2.exists() && balance3.exists() &&
                                                        balance4.exists() && balance5.exists() &&
                                                        balance1.value == balance2.value &&
                                                        balance2.value == balance3.value &&
                                                        balance3.value == balance4.value &&
                                                        balance4.value == balance5.value) {

                                                        if (blockchain1.exists() && blockchain2.exists() &&
                                                            blockchain3.exists() && blockchain4.exists() &&
                                                            blockchain5.exists() &&
                                                            !(blockchain1.value == blockchain2.value &&
                                                                    blockchain2.value == blockchain3.value &&
                                                                    blockchain3.value == blockchain4.value &&
                                                                    blockchain4.value == blockchain5.value)
                                                        ) {
                                                            Toast.makeText(
                                                                this@SignUpActivity,
                                                                "Someone Has Corrupted Data. Please Try Again To Sign Up",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        } else {
                                                            databaseReference.child("miners")
                                                                .child(st_phone)
                                                                .child("Users_Balance")
                                                                .setValue(balance1.value)

                                                            val transactions =
                                                                snapshot.child(random3)
                                                                    .child("transactions")

                                                            databaseReference.child("miners")
                                                                .child(st_phone)
                                                                .child("transactions")
                                                                .setValue(transactions.value)

                                                            databaseReference.child("miners")
                                                                .child(st_phone)
                                                                .child("transactions")
                                                                .addListenerForSingleValueEvent(
                                                                    object :
                                                                        ValueEventListener {
                                                                        override fun onDataChange(
                                                                            snapshot: DataSnapshot
                                                                        ) {
                                                                            for (childSnapshot in snapshot.children) {
                                                                                val id =
                                                                                    childSnapshot.key
                                                                                if (childSnapshot.child(
                                                                                        "Block_No"
                                                                                    )
                                                                                        .exists()
                                                                                ) {
                                                                                    databaseReference.child(
                                                                                        "miners"
                                                                                    )
                                                                                        .child(
                                                                                            st_phone
                                                                                        )
                                                                                        .child("transactions")
                                                                                        .child(id.toString())
                                                                                        .child("Status")
                                                                                        .setValue("Blocked")
                                                                                } else {
                                                                                    databaseReference.child(
                                                                                        "miners"
                                                                                    )
                                                                                        .child(
                                                                                            st_phone
                                                                                        )
                                                                                        .child("transactions")
                                                                                        .child(id.toString())
                                                                                        .child("Status")
                                                                                        .setValue("Unrecognized")
                                                                                }
                                                                            }
                                                                        }

                                                                        override fun onCancelled(
                                                                            error: DatabaseError
                                                                        ) {
                                                                        }
                                                                    })


                                                            if (blockchain1.exists() && blockchain2.exists() &&
                                                                blockchain1.value == blockchain2.value
                                                            ) {
                                                                databaseReference.child("miners")
                                                                    .child(st_phone)
                                                                    .child("main_blockchain")
                                                                    .setValue(blockchain1.value)

                                                                val lastChild =
                                                                    blockchain2.children.last()
                                                                databaseReference.child("miners")
                                                                    .child(st_phone)
                                                                    .child("blockchain")
                                                                    .child(lastChild.key.toString())
                                                                    .setValue(lastChild.value)
                                                            }

                                                            databaseReference.child("miners")
                                                                .child(st_phone)
                                                                .child("User_Name")
                                                                .setValue(st_name)
                                                            databaseReference.child("miners")
                                                                .child(st_phone)
                                                                .child("Account").setValue(st_phone)
                                                            databaseReference.child("miners")
                                                                .child(st_phone)
                                                                .child("Public_Key")
                                                                .setValue(
                                                                    Base64.getEncoder()
                                                                        .encodeToString(publicKey.encoded)
                                                                )
                                                            databaseReference.child("miners")
                                                                .child(st_phone)
                                                                .child("Private_Key")
                                                                .setValue(r_privateKey)

                                                            databaseReference.child("miners")
                                                                .addListenerForSingleValueEvent(
                                                                    object : ValueEventListener {
                                                                        override fun onDataChange(
                                                                            snapshot: DataSnapshot
                                                                        ) {
                                                                            for (childSnapshot in snapshot.children) {
                                                                                val phone =
                                                                                    childSnapshot.key

                                                                                databaseReference.child(
                                                                                    "miners"
                                                                                )
                                                                                    .child(phone.toString())
                                                                                    .child("Users_Balance")
                                                                                    .child(st_phone)
                                                                                    .child("Initial")
                                                                                    .setValue(100.0)
                                                                                databaseReference.child(
                                                                                    "miners"
                                                                                )
                                                                                    .child(phone.toString())
                                                                                    .child("Users_Balance")
                                                                                    .child(st_phone)
                                                                                    .child("Sent")
                                                                                    .setValue(0.0)
                                                                            }
                                                                        }

                                                                        override fun onCancelled(
                                                                            error: DatabaseError
                                                                        ) {

                                                                        }
                                                                    })

                                                            val userReference =
                                                                databaseReference.child("PublicKeys")
                                                                    .child(st_phone)
                                                            userReference.child("User_Name")
                                                                .setValue(st_name)
                                                            userReference.child("Account")
                                                                .setValue(st_phone)
                                                            userReference.child("Public_Key")
                                                                .setValue(
                                                                    Base64.getEncoder()
                                                                        .encodeToString(publicKey.encoded)
                                                                )

                                                            Toast.makeText(
                                                                this@SignUpActivity,
                                                                "Your Account No. Is " + st_phone,
                                                                Toast.LENGTH_SHORT
                                                            ).show()

                                                            val intent = Intent(
                                                                this@SignUpActivity,
                                                                SignInActivity::class.java
                                                            )
                                                            //startActivity(intent)
                                                            //finish()
                                                        }
                                                    } else {
                                                        Toast.makeText(
                                                            this@SignUpActivity,
                                                            "Someone Has Corrupted Data. Please Try Again To Sign Up",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }

                                            override fun onCancelled(error: DatabaseError) {

                                            }
                                        })

                                } else {
                                    Toast.makeText(
                                        this,
                                        "Authentication failed.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                val endTime = System.currentTimeMillis()
                                val timeRequiredForMining = endTime - startTime

                                timeText.text = "$timeRequiredForMining ms"
                            }
                    }
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveNewUser(st_name: String, st_phone: String, publicKey: Any) {
        databaseReference.child("miners").child(st_phone).child("User_Name").setValue(st_name)
        databaseReference.child("miners").child(st_phone).child("Account").setValue(st_phone)
        databaseReference.child("miners").child(st_phone).child("Public_Key")
            .setValue(publicKey)
        databaseReference.child("miners").child(st_phone).child("Private_Key").setValue(r_privateKey)

        databaseReference.child("miners").child(st_phone).child("Users_Balance")
            .child(st_phone).child("Initial").setValue(100.0)
        databaseReference.child("miners").child(st_phone).child("Users_Balance")
            .child(st_phone).child("Sent").setValue(0.0)

        val userReference = databaseReference.child("PublicKeys").child(st_phone)
        userReference.child("User_Name").setValue(st_name)
        userReference.child("Account").setValue(st_phone)
        userReference.child("Public_Key").setValue(publicKey)

        val intent = Intent(this@SignUpActivity, SignInActivity::class.java)
        startActivity(intent)
    }

    private fun generateKeyPair(): KeyPair {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048)
        return keyGen.generateKeyPair()
    }

}