package com.example.wisechoice

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.wisechoice.R
import java.security.MessageDigest

class TransactionDetailsActivity : AppCompatActivity() {

    private lateinit var st_id: String
    private lateinit var st_activity: String
    private lateinit var transactionIDTextView: TextView
    private lateinit var senderTextView: TextView
    private lateinit var signatureTextView: TextView
    private lateinit var receiverTextView: TextView
    private lateinit var amountTextView: TextView
    private lateinit var feesTextView: TextView
    private lateinit var statusTextView: TextView
    private lateinit var transactionTimeTextView: TextView
    private lateinit var blockNoTextView: TextView

    private lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_details)

        transactionIDTextView = findViewById(R.id.transaction_id)
        senderTextView = findViewById(R.id.sender)
        signatureTextView = findViewById(R.id.signature)
        receiverTextView = findViewById(R.id.receiver)
        amountTextView = findViewById(R.id.amount)
        feesTextView = findViewById(R.id.fees)
        statusTextView = findViewById(R.id.status)
        transactionTimeTextView = findViewById(R.id.transaction_time)
        blockNoTextView = findViewById(R.id.block_no)

        st_id = intent.getStringExtra("transaction_id") ?: ""
        st_activity = intent.getStringExtra("activity") ?: ""

        val sharedPreferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        fetchTransactionDetails(st_phone)
    }

    private fun fetchTransactionDetails(st_phone: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child("transactions").child(st_id)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val transactionID = snapshot.child("Transaction_ID").value.toString()
                    val sender = snapshot.child("Sender").value.toString()
                    val receiver = snapshot.child("Receiver").value.toString()
                    val signature = snapshot.child("Signature").value.toString()
                    val amount = snapshot.child("Amount").value.toString()
                    val fees = snapshot.child("Fees").value.toString()
                    val status = snapshot.child("Status").value.toString()
                    val transactionTime = snapshot.child("Transaction_Time").value.toString()
                    val blockNo = snapshot.child("Block_No").value.toString()

                    transactionIDTextView.text = transactionID
                    if(st_activity == "activity" || sender == st_phone || receiver == st_phone) {
                        senderTextView.text = sender
                        receiverTextView.text = receiver
                    }
                    else {
                        senderTextView.text = hashText(sender)
                        receiverTextView.text = hashText(receiver)
                    }
                    signatureTextView.text = signature
                    amountTextView.text = amount
                    feesTextView.text = fees
                    statusTextView.text = status
                    transactionTimeTextView.text = transactionTime
                    blockNoTextView.text = blockNo
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun hashText(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(text.toByteArray())
        return bytesToHex(hashedBytes)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
        }
        return String(hexChars)
    }
}