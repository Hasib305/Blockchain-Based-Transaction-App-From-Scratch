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

class BlockDetailsAdapter(
    private val context: Context,
    private val senders: List<String>,
    private val receivers: List<String>,
    private val amounts: List<String>,
    private val feeses: List<String>,
    private val verifies: List<String>,
    private val ids: List<String>,
    private val signatures: List<String>,
    private val transaction_times: List<String>,
    private val st_id: String,

    private var databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference()

) : RecyclerView.Adapter<BlockDetailsAdapter.MyViewHolder>() {

    private fun retrieveSenderPublicKey(senderPhone: String, callback: (String?) -> Unit) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("PublicKeys").child(senderPhone)

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val publicKey = snapshot.child("Public_Key").getValue(String::class.java)
                callback(publicKey)
            }

            override fun onCancelled(error: DatabaseError) {
                callback(null)
            }
        })
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun verifySignature(publicKey: String, signature: String, data: String): Boolean {
        try {

            //Log.d("SignatureVerification", "Public Key: $publicKey")
            //Log.d("SignatureVerification", "Signature: $signature")
            //Log.d("SignatureVerification", "Data: $data")

            val publicBytes = Base64.getDecoder().decode(publicKey)
            val keySpec = X509EncodedKeySpec(publicBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val public_key = keyFactory.generatePublic(keySpec)

            val signatureBytes = Base64.getDecoder().decode(signature)
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initVerify(public_key)
            signature.update(data.toByteArray())

            return signature.verify(signatureBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.temp_block_xml, parent, false)
        return MyViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
        holder.verify.text = "${verifies[position]}"
        val idValue = ids[position]
        val signatureValue = signatures[position]
        val timeval = transaction_times[position]

        if(verifies[position] == "Unrecognized") {
            holder.verify_button.isEnabled = true
            holder.verify_button.text = "Verify"
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }

        else if(verifies[position] == "Verified") {
            holder.verify_button.isEnabled = false
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
        }

        else if(verifies[position] == "Not Verified") {
            holder.verify_button.isEnabled = false
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_green ))
        }

        holder.verify_button.setOnClickListener {
            val sharedPreferences =
                context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
            val st_phone = sharedPreferences.getString("Account", "") ?: ""

            if(holder.verify.text == "Unrecognized") {
                retrieveSenderPublicKey(senders[position]) { publicKey ->
                    if (publicKey != null) {
                        val signatureVerified = verifySignature(publicKey, signatures[position], "${receivers[position]}${amounts[position]}${feeses[position]}$timeval")
                        if (signatureVerified) {
                            Toast.makeText(context, "Signature Verified.\nSignature Algorithm: SHA256withRSA\nKey-Factory Algorithm: RSA", Toast.LENGTH_LONG).show()

                            val newTransactionRef = databaseReference.child("miners").child(st_phone)
                                .child("block_queue").child(st_id).child("transaction_details").child(idValue)
                            newTransactionRef.child("Status").setValue("Verified")

                            holder.verify.text = "Verified"
                            holder.verify_button.isEnabled = true
                            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                        }

                        else {
                            val newTransactionRef = databaseReference.child("miners").child(st_phone)
                                .child("block_queue").child(st_id).child("transaction_details").child(idValue)
                            newTransactionRef.child("Status").setValue("Not Verified")

                            holder.verify.text = "Not Verified"
                            holder.verify_button.isEnabled = true
                            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_green))
                        }
                    }
                }
            }
        }

        holder.transaction_card.setOnClickListener {

            val intent = Intent(context, TransactionDetailsActivity::class.java)

            intent.putExtra("transaction_id", idValue)
            intent.putExtra("activity", "not_inbox")
            context.startActivity(intent)
        }
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

    override fun getItemCount(): Int {
        return senders.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var sender: TextView = itemView.findViewById(R.id.sender)
        var receiver: TextView = itemView.findViewById(R.id.receiver)
        var amount: TextView = itemView.findViewById(R.id.amount)
        var fees: TextView = itemView.findViewById(R.id.fees)
        var verify: TextView = itemView.findViewById(R.id.verify)
        var verify_button: Button = itemView.findViewById(R.id.verify_button)
        var transaction_card = itemView.findViewById<CardView>(R.id.transaction_card)
    }
}

class BlockDetailsActivity : AppCompatActivity() {

    private lateinit var st_id: String
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
    private lateinit var acceptButton: Button
    private lateinit var timeText: TextView

    private lateinit var databaseReference: DatabaseReference

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterClass: BlockDetailsAdapter

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
        setContentView(R.layout.activity_block_details)

        blockIDTextView = findViewById(R.id.block_id)
        blockHashTextView = findViewById(R.id.block_hash)
        previousHashTextView = findViewById(R.id.previous_hash)
        nonceTextView = findViewById(R.id.nonce)
        minerTextView = findViewById(R.id.miner)
        noOfTransactionsTextView = findViewById(R.id.no_of_transactions)
        totalSentTextView = findViewById(R.id.total_sent)
        sizeTextView = findViewById(R.id.size)
        totalFeesTextView = findViewById(R.id.total_fees)
        minedTextView = findViewById(R.id.mined)
        acceptButton = findViewById(R.id.accept_button)
        timeText = findViewById(R.id.time_text)

        st_id = intent.getStringExtra("block_id") ?: ""

        val sharedPreferences = getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        fetchTransactionDetails(st_phone)

        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child("block_queue").child(st_id).child("transaction_details")

        recyclerView = findViewById(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapterClass = BlockDetailsAdapter(
            this,
            senders,
            receivers,
            amounts,
            feeses,
            verifies,
            ids,
            signatures,
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

    private fun fetchTransactionDetails(st_phone: String) {
        val sharedPreferences = this.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child("block_queue").child(st_id)

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

                    acceptButton.setOnClickListener {
                        acceptBlock()
                    }
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

    private fun acceptBlock() {
        val startTime = System.currentTimeMillis()

        val sharedPreferences = this.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        val blockchainReference =
            FirebaseDatabase.getInstance().getReference("miners")
                .child(st_phone).child("blockchain").child(st_id)

        FirebaseDatabase.getInstance().getReference("miners")
            .child(st_phone).child("notifications").child(st_id).removeValue()

        FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child("block_queue").child(st_id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val transactionDetailsSnapshot =
                            snapshot.child("transaction_details")

                        val allVerified =
                            transactionDetailsSnapshot.children.all { transactionSnapshot ->
                                transactionSnapshot.child("Status")
                                    .getValue(String::class.java) == "Verified"
                            }

                        val anyNotVerified =
                            transactionDetailsSnapshot.children.any { transactionSnapshot ->
                                transactionSnapshot.child("Status")
                                    .getValue(String::class.java) == "Not Verified"
                            }

                        if (anyNotVerified) {
                            Toast.makeText(
                                this@BlockDetailsActivity,
                                "Corrupted Block Can't Be Added To Blockchain."
                                        + "Please Try Another Block From Block Queue",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else if (allVerified) {
                            FirebaseDatabase.getInstance().getReference("miners")
                                .child(st_phone)
                                .child("blockchain").child(st_id).removeValue()

                            blockchainReference.setValue(snapshot.value)

                            FirebaseDatabase.getInstance().getReference("miners")
                                .child(st_phone)
                                .child("block_queue").removeValue()

                            Toast.makeText(
                                this@BlockDetailsActivity,
                                "This Block Has Been Added To Your Blockchain Successfully. " +
                                        "Mine The Next Block Of It To Add It To The Main Blockchain.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@BlockDetailsActivity,
                                "Please Make Sure That All Transactions Have Been Verified.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    val endTime = System.currentTimeMillis()
                    val timeRequiredForMining = endTime - startTime

                    timeText.text = "$timeRequiredForMining ms"
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }
}
