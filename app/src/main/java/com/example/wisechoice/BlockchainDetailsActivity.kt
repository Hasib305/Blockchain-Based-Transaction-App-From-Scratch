package com.example.wisechoice

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest

class BlockchainDetailsAdapter(
    private val context: Context,
    private val senders: List<String>,
    private val receivers: List<String>,
    private val amounts: List<String>,
    private val feeses: List<String>,
    private val ids: List<String>,
    private val transaction_times: List<String>,
    private val st_id: String,

    private var databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference()

) : RecyclerView.Adapter<BlockchainDetailsAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.blockchain, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val sharedPreferences = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        if("${senders[position]}" == st_phone || "${receivers[position]}" == st_phone) {
            holder.sender.text = "${senders[position]}"
            holder.receiver.text = "${receivers[position]}"
        }
        else {
            holder.sender.text = "${hashText(senders[position])}.."
            holder.receiver.text = "${hashText(receivers[position])}.."
        }
        holder.amount.text = "${amounts[position]}"
        holder.fees.text = "${feeses[position]}"
        val idValue = ids[position]

        holder.transaction_card.setOnClickListener {

            val intent = Intent(context, TransactionDetailsActivity::class.java)

            intent.putExtra("transaction_id", idValue)
            intent.putExtra("activity", "not_inbox")
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

    private fun hashText(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest(text.toByteArray())
        return bytesToHex(hashedBytes).substring(0, 3)
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

class BlockchainDetailsActivity : AppCompatActivity() {

    private lateinit var st_id: String
    private lateinit var st_path: String
    private lateinit var blockIDTextView: TextView
    private lateinit var blockHashTextView: TextView
    private lateinit var previousHashTextView: TextView
    private lateinit var nonceTextView: TextView
    private lateinit var minerTextView: TextView
    private lateinit var noOfTransactionsTextView: TextView
    private lateinit var totalSentTextView: TextView
    private lateinit var sizeTextView: TextView
    private lateinit var totalFeesTextView: TextView
    private lateinit var minedTextView: TextView

    private lateinit var databaseReference: DatabaseReference

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterClass: BlockchainDetailsAdapter

    private val senders = mutableListOf<String>()
    private val receivers = mutableListOf<String>()
    private val amounts = mutableListOf<String>()
    private val feeses = mutableListOf<String>()
    private val verifies = mutableListOf<String>()
    private val ids = mutableListOf<String>()
    private val transaction_times = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blockchain_details)

        blockIDTextView = findViewById(R.id.block_id)
        blockHashTextView = findViewById(R.id.block_hash)
        previousHashTextView= findViewById(R.id.previous_hash)
        nonceTextView= findViewById(R.id.nonce)
        minerTextView= findViewById(R.id.miner)
        noOfTransactionsTextView= findViewById(R.id.no_of_transactions)
        totalSentTextView= findViewById(R.id.total_sent)
        sizeTextView= findViewById(R.id.size)
        totalFeesTextView= findViewById(R.id.total_fees)
        minedTextView= findViewById(R.id.mined)

        st_id = intent.getStringExtra("block_id") ?: ""
        st_path = intent.getStringExtra("path") ?: ""

        val sharedPreferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        fetchTransactionDetails(st_phone)

        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child(st_path).child(st_id).child("transaction_details")

        recyclerView = findViewById(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapterClass = BlockchainDetailsAdapter(
            this,
            senders,
            receivers,
            amounts,
            feeses,
            ids,
            transaction_times,
            st_id
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
                transaction_times.clear()

                for (dataSnapshot in snapshot.children) {
                    val sender = dataSnapshot.child("Sender").value.toString()
                    val receiver = dataSnapshot.child("Receiver").value.toString()
                    val amount = dataSnapshot.child("Amount").value.toString()
                    val fees = dataSnapshot.child("Fees").value.toString()
                    val verify = dataSnapshot.child("Status").value.toString()
                    val transaction_time = dataSnapshot.child("Transaction_Time").value.toString()
                    val id = dataSnapshot.child("Transaction_ID").value.toString()

                    senders.add(sender)
                    receivers.add(receiver)
                    amounts.add(amount)
                    feeses.add(fees)
                    verifies.add(verify)
                    ids.add(id)
                    transaction_times.add(transaction_time)
                }
                adapterClass.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchTransactionDetails(st_phone: String) {
        val sharedPreferences = this.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child(st_path).child(st_id)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val blockID = st_id
                    val blockHash = snapshot.child("Block_Hash").value.toString()
                    val previousHash = snapshot.child("Previous_Hash").value.toString()
                    val nonce = snapshot.child("Nonce").value.toString()
                    val miner = snapshot.child("Miner").value.toString()
                    val noOfTransactions = snapshot.child("No_Of_Transactions").value.toString()
                    val totalSent = snapshot.child("Total_Amount").value.toString()
                    val size = snapshot.child("Size").value.toString()
                    val totalFees = snapshot.child("Total_Fees").value.toString()
                    val minedTime = snapshot.child("Mined_Time").value.toString()

                    blockIDTextView.text = blockID
                    blockHashTextView.text = blockHash
                    previousHashTextView.text = previousHash
                    nonceTextView.text = nonce
                    if(miner == st_phone) {
                        minerTextView.text = miner
                    }
                    else {
                        minerTextView.text = hashText(miner)
                    }
                    noOfTransactionsTextView.text = noOfTransactions
                    totalSentTextView.text = totalSent
                    sizeTextView.text = size
                    totalFeesTextView.text = totalFees
                    minedTextView.text = minedTime
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