package com.example.wisechoice

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

class InboxAdapter(
    private val context: Context,
    private val senders: List<String>,
    private val receivers: List<String>,
    private val amounts: List<String>,
    private val feeses: List<String>,
    private val verifies: List<String>,
    private val ids: List<String>,
    private val signatures: List<String>,
    private val transaction_times: List<String>,

    private var databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference()

) : RecyclerView.Adapter<InboxAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.blockchain, parent, false)
        return MyViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        holder.sender.text = "${senders[position]}"
        holder.receiver.text = "${receivers[position]}"
        holder.amount.text = "${amounts[position]}"
        holder.fees.text = "${feeses[position]}"
        val idValue = ids[position]
        val signatureValue = signatures[position]
        val timeval = transaction_times[position]

        val sharedPreferences = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        if("${senders[position]}" == st_phone) {
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_gray))
        }
        if("${receivers[position]}" == st_phone) {
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_blue))
        }

        holder.transaction_card.setOnClickListener {

            val intent = Intent(context, TransactionDetailsActivity::class.java)

            intent.putExtra("transaction_id", idValue)
            intent.putExtra("activity", "inbox")
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return senders.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var sender: TextView = itemView.findViewById(R.id.sender)
        var receiver: TextView = itemView.findViewById(R.id.receiver)
        var amount: TextView = itemView.findViewById(R.id.amount)
        var fees: TextView = itemView.findViewById(R.id.fees)
        var transaction_card = itemView.findViewById<CardView>(R.id.transaction_card)
    }
}

class InboxActivity : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterClass: InboxAdapter

    private val senders = mutableListOf<String>()
    private val receivers = mutableListOf<String>()
    private val amounts = mutableListOf<String>()
    private val feeses = mutableListOf<String>()
    private val verifies = mutableListOf<String>()
    private val ids = mutableListOf<String>()
    private val signatures = mutableListOf<String>()
    private val transaction_times = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)

        val sharedPreferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child("inbox")

        recyclerView = findViewById(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapterClass = InboxAdapter(
            this,
            senders,
            receivers,
            amounts,
            feeses,
            verifies,
            ids,
            signatures,
            transaction_times
        )
        recyclerView.adapter = adapterClass

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                senders.clear()
                receivers.clear()
                amounts.clear()
                feeses.clear()
                verifies.clear()
                ids.clear()
                signatures.clear()
                transaction_times.clear()

                for (dataSnapshot in snapshot.children) {
                    val sender = dataSnapshot.child("Sender").value.toString()
                    val receiver = dataSnapshot.child("Receiver").value.toString()
                    val amount = dataSnapshot.child("Amount").value.toString()
                    val fees = dataSnapshot.child("Fees").value.toString()
                    val verify = dataSnapshot.child("Status").value.toString()
                    val transaction_time = dataSnapshot.child("Transaction_Time").value.toString()
                    val id = dataSnapshot.child("Transaction_ID").value.toString()
                    val signature = dataSnapshot.child("Signature").value.toString()

                    senders.add(sender)
                    receivers.add(receiver)
                    amounts.add(amount)
                    feeses.add(fees)
                    verifies.add(verify)
                    ids.add(id)
                    signatures.add(signature)
                    transaction_times.add(transaction_time)
                }
                adapterClass.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}