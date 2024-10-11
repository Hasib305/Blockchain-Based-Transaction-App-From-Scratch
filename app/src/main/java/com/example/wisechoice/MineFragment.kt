package com.example.wisechoice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MineBlockAdapter(
    private val context: Context,
    private val senders: List<String>,
    private val receivers: List<String>,
    private val amounts: List<String>,
    private val feeses: List<String>,
    private val ids: List<String>,
    private val signatures: List<String>,
    private val transaction_times: List<String>,
) : RecyclerView.Adapter<MineBlockAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.mine_block, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val sharedPreferences =
            context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
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

        holder.remove_button.setOnClickListener {
            val sharedPreferences =
                context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
            val st_phone = sharedPreferences.getString("Account", "") ?: ""

            val newTransactionRef = FirebaseDatabase.getInstance().getReference("miners")
                .child(st_phone).child("transactions").child(idValue)
            newTransactionRef.child("Status").setValue("Unrecognized")

            val tempTransactionRef = FirebaseDatabase.getInstance().getReference("miners")
                .child(st_phone).child("temporary_blocks").child(idValue)
            tempTransactionRef.removeValue()
        }

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

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var sender: TextView = itemView.findViewById(R.id.sender)
        var receiver: TextView = itemView.findViewById(R.id.receiver)
        var amount: TextView = itemView.findViewById(R.id.amount)
        var fees: TextView = itemView.findViewById(R.id.fees)
        var remove_button: Button = itemView.findViewById(R.id.remove_button)
        var transaction_card = itemView.findViewById<CardView>(R.id.transaction_card)
    }
}

class MineFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterClass: MineBlockAdapter
    private lateinit var mineButton: Button
    private lateinit var hashText: TextView
    private lateinit var timeText: TextView

    private val senders = mutableListOf<String>()
    private val receivers = mutableListOf<String>()
    private val amounts = mutableListOf<String>()
    private val feeses = mutableListOf<String>()
    private val ids = mutableListOf<String>()
    private val signatures = mutableListOf<String>()
    private val transaction_times = mutableListOf<String>()

    var drawerLayout: DrawerLayout? = null
    var navigationView: NavigationView? = null
    var nView: View? = null

    var username: TextView? = null
    var phone: TextView? = null
    var home_menu: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mine, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawerLayout = view.findViewById<DrawerLayout>(R.id.drawer)

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            requireActivity(), drawerLayout,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        actionBarDrawerToggle.syncState()

        navigationView = view.findViewById<NavigationView>(R.id.navigation)
        nView = navigationView?.getHeaderView(0)
        username = nView?.findViewById<TextView>(R.id.username)
        phone = nView?.findViewById<TextView>(R.id.phone)
        home_menu = view.findViewById<ImageView>(R.id.home_menu)

        home_menu?.setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        navigationView?.setNavigationItemSelectedListener(this)

        val sharedPreferences = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        phone?.text = st_phone
        FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userName = snapshot.child("User_Name").getValue().toString()

                    username?.text = userName
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })


        recyclerView = view.findViewById(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapterClass = MineBlockAdapter(
            requireContext(),
            senders,
            receivers,
            amounts,
            feeses,
            ids,
            signatures,
            transaction_times
        )
        recyclerView.adapter = adapterClass

        mineButton = view.findViewById(R.id.mine_button)

        hashText = view.findViewById(R.id.hash_text)
        timeText = view.findViewById(R.id.time_text)

        mineButton.setOnClickListener {
            val st_phone = sharedPreferences.getString("Account", "") ?: ""

            val tempBlockchainRef = FirebaseDatabase.getInstance().getReference("miners")
                .child(st_phone).child("temporary_blocks")

            tempBlockchainRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(
                            context,
                            "At Least One Transaction Is Required To Mine A Block",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (snapshot.exists()) {

                        val blockchainRef = FirebaseDatabase.getInstance().getReference("miners")
                            .child(st_phone)
                            .child("blockchain")

                        blockchainRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (!snapshot.exists()) {
                                    performMining()
                                    removeNulls()
                                } else {
                                    checkBlock()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }

        checkBlockchain("main_blockchain")
        checkBlockchain("blockchain")

        val tempBlocksReference = FirebaseDatabase.getInstance().getReference("miners")
            .child(st_phone).child("temporary_blocks")

        tempBlocksReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                senders.clear()
                receivers.clear()
                amounts.clear()
                feeses.clear()
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
                    ids.add(id)
                    signatures.add(signature)
                    transaction_times.add(transaction_time)
                }
                adapterClass.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
        removeNulls()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun performMining() {
        val sharedPreferences = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)

        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        val concatenatedTransactions = StringBuilder()

        var total_amount = 0.0
        var total_fees = 0.0

        for (idValue in ids) {
            val sender = senders[ids.indexOf(idValue)]
            val receiver = receivers[ids.indexOf(idValue)]
            val amount = amounts[ids.indexOf(idValue)]
            val fees = feeses[ids.indexOf(idValue)]

            val amountFloat = amount?.toFloatOrNull() ?: 0.0f
            val feesFloat = fees?.toFloatOrNull() ?: 0.0f

            total_amount += amountFloat
            total_fees += feesFloat

            val transactionInfo =
                "Sender: $sender, Receiver: $receiver, Amount: $amount, Fees: $fees"
            concatenatedTransactions.append(transactionInfo)
        }

        val concatenatedString = concatenatedTransactions.toString()

        if (concatenatedString.toString().length != 0) {

            var hashedString: String
            var randomNotch: Int

            Thread {
                val startTime = System.currentTimeMillis()

                do {
                    randomNotch = (10000..99999).random()
                    val stringWithNotch = "$randomNotch$st_phone$concatenatedString$total_amount$total_fees"

                    hashedString = hashString(stringWithNotch)

                    requireActivity().runOnUiThread() {
                        hashText.text = "Hash: $hashedString"
                    }

                    Thread.sleep(10)

                } while (!hashedString.startsWith("00"))

                val endTime = System.currentTimeMillis()
                val timeRequiredForMining = endTime - startTime

                timeText.text = "Time Required: $timeRequiredForMining ms"


                if(concatenatedString.toString().length != 0) {
                    val temporaryBlockRef = FirebaseDatabase.getInstance().getReference("miners")
                        .child(st_phone)
                        .child("temporary_blocks")

                    temporaryBlockRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                setValues(hashedString, randomNotch) 
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {

                        }
                    })
                }

            }.start()
        }
    }

    private fun removeNulls() {
        FirebaseDatabase.getInstance().getReference("miners")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (childSnapshot in snapshot.children) {
                        val phone = childSnapshot.key

                        FirebaseDatabase.getInstance().getReference("miners")
                            .child(phone.toString())
                            .child("block_queue").addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (childSnapshot in snapshot.children) {
                                        val block = childSnapshot.key
                                        val no = childSnapshot.child("transaction_details")
                                        if(!no.exists()) {
                                            FirebaseDatabase.getInstance().getReference("miners")
                                                .child(phone.toString())
                                                .child("block_queue").child(block.toString()).removeValue()
                                        }
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {

                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setValues(hashedString: String, randomNotch: Int) {

        val sharedPreferences = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)

        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        val blockVal = FirebaseDatabase.getInstance().getReference("miners")
            .child(st_phone).child("block_queue").push()

        blockVal.child("Block_ID").setValue(blockVal.key)
        blockVal.child("Block_Hash").setValue("$hashedString")
        blockVal.child("Nonce").setValue(randomNotch)
        blockVal.child("Miner").setValue(st_phone)
        blockVal.child("Size")
            .setValue(((hashedString.length - 5) * 4).toString())
        blockVal.child("Previous_Hash")
            .setValue("000000000000000000000000000000000000000000000000000000000000000000")

        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)

        blockVal.child("Mined_Time").setValue(formattedDateTime.toString())


        val minersRef = FirebaseDatabase.getInstance().getReference("miners")

        minersRef.child(st_phone).child("blockchain")
            .limitToLast(1)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (childSnapshot in dataSnapshot.children) {

                        val previousBlockID = childSnapshot.key
                        val previousMiner = childSnapshot.child("Miner").value
                        val previousTotalAmount =
                            childSnapshot.child("Total_Amount").value
                        val previousTotalFees =
                            childSnapshot.child("Total_Fees").value
                        val previousHash =
                            childSnapshot.child("Block_Hash").value
                        val prevPreviousHash =
                            childSnapshot.child("Previous_Hash").value

                        blockVal.child("Previous_Hash").setValue(previousHash)

                        val minersRef =
                            FirebaseDatabase.getInstance()
                                .getReference("miners")

                        minersRef.addListenerForSingleValueEvent(object :
                            ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (childSnapshot in snapshot.children) {
                                    val phone = childSnapshot.key

                                    if (phone != null) {

                                        val currentDateTime =
                                            LocalDateTime.now()
                                        val formatter =
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                        val formattedDateTime =
                                            currentDateTime.format(formatter)

                                        val reference =
                                            FirebaseDatabase.getInstance()
                                                .getReference("miners")
                                                .child(phone)
                                                .child("notifications")

                                        reference.child(previousBlockID.toString())
                                            .child("Message")
                                            .setValue(previousMiner)

                                        reference.child(blockVal.key.toString())
                                            .child("Message")
                                            .setValue(st_phone)

                                        reference.child(blockVal.key.toString())
                                            .child("Notification_Time")
                                            .setValue(formattedDateTime.toString())

                                        reference.child(previousBlockID.toString())
                                            .child("Notification_Time")
                                            .setValue(formattedDateTime.toString())

                                        reference.child(blockVal.key.toString())
                                            .child("Status")
                                            .setValue("Unread")

                                        reference.child(previousBlockID.toString())
                                            .child("Status")
                                            .setValue("Unread")

                                        reference.child(blockVal.key.toString())
                                            .child("Activity")
                                            .setValue("BlockDetailsActivity")

                                        reference.child(previousBlockID.toString())
                                            .child("Activity")
                                            .setValue("BlockchainDetailsActivity")

                                        reference.child(blockVal.key.toString())
                                            .child("Notification_ID")
                                            .setValue(blockVal.key.toString())

                                        reference.child(previousBlockID.toString())
                                            .child("Notification_ID")
                                            .setValue(previousBlockID.toString())

                                        FirebaseDatabase.getInstance()
                                            .getReference("miners")
                                            .child(phone).child("blockchain")
                                            .removeValue()

                                        FirebaseDatabase.getInstance()
                                            .getReference("miners")
                                            .child(phone).child("blockchain")
                                            .child(previousBlockID.toString())
                                            .setValue(
                                                dataSnapshot.child(
                                                    previousBlockID.toString()
                                                ).value
                                            )

                                        FirebaseDatabase.getInstance()
                                            .getReference("miners")
                                            .child(phone)
                                            .child("main_blockchain")
                                            .child(previousBlockID.toString())
                                            .setValue(
                                                dataSnapshot.child(
                                                    previousBlockID.toString()
                                                ).value
                                            )

                                        val databaseReference =
                                            FirebaseDatabase.getInstance()
                                                .getReference("miners")
                                                .child(phone)
                                                .child("main_blockchain")
                                                .child(previousBlockID.toString())
                                                .child("transaction_details")

                                        databaseReference.addListenerForSingleValueEvent(
                                            object : ValueEventListener {
                                                override fun onDataChange(
                                                    dataSnapshot: DataSnapshot
                                                ) {
                                                    for (transactionSnapshot in dataSnapshot.children) {

                                                        val transactionID =
                                                            transactionSnapshot.key
                                                        val amount =
                                                            transactionSnapshot.child(
                                                                "Amount"
                                                            ).value.toString()
                                                        val fees =
                                                            transactionSnapshot.child(
                                                                "Fees"
                                                            ).value.toString()
                                                        val sender =
                                                            transactionSnapshot.child(
                                                                "Sender"
                                                            ).value.toString()
                                                        val receiver =
                                                            transactionSnapshot.child(
                                                                "Receiver"
                                                            ).value.toString()

                                                        val inboxRef = FirebaseDatabase.getInstance()
                                                            .getReference("miners").child(receiver)
                                                            .child("inbox")
                                                            .child(transactionID.toString())

                                                        inboxRef.child("Amount").setValue(amount)
                                                        inboxRef.child("Fees").setValue(fees)
                                                        inboxRef.child("Sender").setValue(sender)
                                                        inboxRef.child("Receiver").setValue(receiver)

                                                        val receiverBalanceRef =
                                                            FirebaseDatabase.getInstance()
                                                                .getReference("miners")
                                                                .child(phone)
                                                                .child("Users_Balance")
                                                                .child(receiver)
                                                                .child("Received_Amount")

                                                        receiverBalanceRef.child(
                                                            transactionID.toString()
                                                        ).setValue(amount)

                                                        val feesBalanceRef =
                                                            FirebaseDatabase.getInstance()
                                                                .getReference("miners")
                                                                .child(phone)
                                                                .child("Users_Balance")
                                                                .child(receiver)
                                                                .child("Fees_Amount")

                                                        feesBalanceRef.child(
                                                            transactionID.toString()
                                                        ).setValue(fees)

                                                        val previousMinerBalanceRef =
                                                            FirebaseDatabase.getInstance()
                                                                .getReference("miners")
                                                                .child(phone)
                                                                .child("Users_Balance")
                                                                .child(previousMiner.toString())
                                                                .child("Mined_Amount")

                                                        previousMinerBalanceRef.child(
                                                            transactionID.toString()
                                                        ).setValue("5")
                                                    }
                                                }

                                                override fun onCancelled(
                                                    databaseError: DatabaseError
                                                ) {

                                                }
                                            })

                                        FirebaseDatabase.getInstance()
                                            .getReference("miners")
                                            .child(phone).child("block_queue")
                                            .addListenerForSingleValueEvent(
                                                object :
                                                    ValueEventListener {
                                                    override fun onDataChange(
                                                        dataSnapshot: DataSnapshot
                                                    ) {
                                                        for (childSnapshot in dataSnapshot.children) {
                                                            val ID =
                                                                childSnapshot.key
                                                            val blockHash =
                                                                childSnapshot.child(
                                                                    "Previous_Hash"
                                                                ).value

                                                            if (blockHash == prevPreviousHash) {
                                                                if (ID.toString() != previousBlockID.toString()) {
                                                                    FirebaseDatabase.getInstance()
                                                                        .getReference(
                                                                            "miners"
                                                                        )
                                                                        .child(
                                                                            st_phone
                                                                        )
                                                                        .child("rejected_blocks")
                                                                        .child(
                                                                            ID.toString()
                                                                        )
                                                                        .setValue(
                                                                            dataSnapshot.child(
                                                                                ID.toString()
                                                                            ).value
                                                                        )
                                                                }

                                                                if (ID.toString() != previousBlockID.toString()) {
                                                                    FirebaseDatabase.getInstance()
                                                                        .getReference(
                                                                            "miners"
                                                                        )
                                                                        .child(
                                                                            phone
                                                                        )
                                                                        .child("rejected_blocks")
                                                                        .child(
                                                                            ID.toString()
                                                                        )
                                                                        .setValue(
                                                                            dataSnapshot.child(
                                                                                ID.toString()
                                                                            ).value
                                                                        )
                                                                }

                                                                FirebaseDatabase.getInstance()
                                                                    .getReference(
                                                                        "miners"
                                                                    )
                                                                    .child(phone)
                                                                    .child("block_queue")
                                                                    .child(ID.toString())
                                                                    .removeValue()

                                                                FirebaseDatabase.getInstance()
                                                                    .getReference(
                                                                        "miners"
                                                                    )
                                                                    .child(phone)
                                                                    .child("notifications")
                                                                    .child(ID.toString())
                                                                    .removeValue()
                                                            }
                                                        }
                                                    }

                                                    override fun onCancelled(
                                                        error: DatabaseError
                                                    ) {

                                                    }
                                                })
                                    }
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

        val blockRef = blockVal.child("transaction_details")
        var noOfTransactions = 0
        var totalAmount = 0.0
        var totalFees = 0.0

        for (idValue in ids) {
            val transactionRef =
                FirebaseDatabase.getInstance().getReference("miners")
                    .child(st_phone).child("transactions")

            val query =
                transactionRef.orderByChild("Transaction_ID").equalTo(idValue)
            query.addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(transactionSnapshot: DataSnapshot) {
                    for (transactionData in transactionSnapshot.children) {
                        val transactionId = transactionData.key.toString()
                        val transactionVerifyRef =
                            transactionRef.child(transactionId)
                                .child("Status")

                        transactionVerifyRef.setValue("Unrecognized")

                        noOfTransactions++

                        val index = ids.indexOf(idValue)
                        if (index != -1) {
                            val amount = amounts[index].toDoubleOrNull() ?: 0.0
                            totalAmount += amount

                            val fees = feeses[index].toDoubleOrNull() ?: 0.0
                            totalFees += fees
                        }

                        blockRef.child(transactionId).child("Transaction_ID")
                            .setValue(ids[ids.indexOf(idValue)])
                        blockRef.child(transactionId).child("Sender")
                            .setValue(senders[ids.indexOf(idValue)])
                        blockRef.child(transactionId).child("Receiver")
                            .setValue(receivers[ids.indexOf(idValue)])
                        blockRef.child(transactionId).child("Amount")
                            .setValue(amounts[ids.indexOf(idValue)])
                        blockRef.child(transactionId).child("Fees")
                            .setValue(feeses[ids.indexOf(idValue)])
                        blockRef.child(transactionId).child("Signature")
                            .setValue(signatures[ids.indexOf(idValue)])
                        blockRef.child(transactionId).child("Transaction_Time")
                            .setValue(transaction_times[ids.indexOf(idValue)])
                        blockRef.child(transactionId).child("Status")
                            .setValue("Unrecognized")

                        blockVal.child("No_Of_Transactions")
                            .setValue(noOfTransactions)
                        blockVal.child("Total_Amount").setValue(totalAmount)
                        blockVal.child("Total_Fees").setValue(totalFees)
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })

            val tempTransactionRef =
                FirebaseDatabase.getInstance().getReference("miners")
                    .child(st_phone).child("temporary_blocks").child(idValue)

            tempTransactionRef.removeValue()
        }

        minersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (childSnapshot in dataSnapshot.children) {
                    val childKey = childSnapshot.key

                    val blockchainReference =
                        FirebaseDatabase.getInstance().getReference("miners")
                            .child(childKey.toString()).child("block_queue")
                            .child(blockVal.key.toString())

                    FirebaseDatabase.getInstance().getReference("miners")
                        .child(st_phone)
                        .child("block_queue").child(blockVal.key.toString())
                        .addListenerForSingleValueEvent(object :
                            ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                if (snapshot.exists()) {
                                    blockchainReference.setValue(snapshot.value)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {

                            }
                        })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    private fun checkBlock() {
        val sharedPreferences =
            requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        val minersRef = FirebaseDatabase.getInstance().getReference("miners")

        minersRef.child(st_phone).child("blockchain")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (childSnapshot in dataSnapshot.children) {
                        val previousBlockID = childSnapshot.key
                        val previousMiner = childSnapshot.child("Miner").value
                        val previousHash = childSnapshot.child("Block_Hash").value
                        val previousNonce = childSnapshot.child("Nonce").value

                        FirebaseDatabase.getInstance().getReference("miners")
                            .child(st_phone).child("blockchain").child(previousBlockID.toString())
                            .child("transaction_details")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                @RequiresApi(Build.VERSION_CODES.O)
                                override fun onDataChange(dataSnapshot: DataSnapshot) {
                                    val transactionInfoList = mutableListOf<String>()

                                    var total_amount = 0.0
                                    var total_fees = 0.0

                                    for (grandSnapshot in dataSnapshot.children) {
                                        val sender = grandSnapshot.child("Sender")
                                            .getValue(String::class.java)
                                        val receiver = grandSnapshot.child("Receiver")
                                            .getValue(String::class.java)
                                        val amount = grandSnapshot.child("Amount").getValue(String::class.java)
                                        val fees = grandSnapshot.child("Fees").getValue(String::class.java)

                                        val amountFloat = amount?.toFloatOrNull() ?: 0.0f
                                        val feesFloat = fees?.toFloatOrNull() ?: 0.0f

                                        total_amount += amountFloat
                                        total_fees += feesFloat

                                        val grandChildInfo =
                                            "Sender: $sender, Receiver: $receiver, Amount: $amount, Fees: $fees"

                                        transactionInfoList.add(grandChildInfo)
                                    }

                                    val finalTransactionInfo =
                                        transactionInfoList.joinToString(separator = "")

                                    val hashed =
                                        hashString("$previousNonce$previousMiner$finalTransactionInfo$total_amount$total_fees")

                                    if (hashed == previousHash) {
                                        performMining()
                                        return
                                    } else {
                                       Toast.makeText(
                                            context,
                                            "Corrupted Block Can't Be Added To Blockchain. " +
                                                    "Try With Another Block From Your Block Queue.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return
                                    }
                                }

                                override fun onCancelled(databaseError: DatabaseError) {
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    private fun checkBlockchain(path: String) {
        val sharedPreferences = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        FirebaseDatabase.getInstance()
            .getReference("miners")
            .child(st_phone)
            .child(path)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (childSnapshot in snapshot.children) {
                        val blockKey = childSnapshot.key
                        val blockTransactionDetails = childSnapshot.child("transaction_details")

                        blockTransactionDetails.children.forEach { transactionSnapshot ->
                            val transactionKey = transactionSnapshot.key


                                FirebaseDatabase.getInstance()
                                    .getReference("miners")
                                    .child(st_phone)
                                    .child("temporary_blocks")
                                    .child(transactionKey.toString())
                                    .removeValue()

                            var st_status = transactionSnapshot.child("Block_No")

                            if(st_status != null) {
                                FirebaseDatabase.getInstance()
                                    .getReference("miners")
                                    .child(st_phone)
                                    .child("transactions")
                                    .child(transactionKey.toString())
                                    .child("Status")
                                    .setValue("Blocked")
                            }

                            else {
                                FirebaseDatabase.getInstance()
                                    .getReference("miners")
                                    .child(st_phone)
                                    .child("transactions")
                                    .child(transactionKey.toString())
                                    .child("Status")
                                    .setValue("Temporary Blocked")
                            }

                            if(path == "main_blockchain") {
                                FirebaseDatabase.getInstance()
                                    .getReference("miners")
                                    .child(st_phone)
                                    .child("transactions")
                                    .child(transactionKey.toString())
                                    .child("Block_No")
                                    .setValue(blockKey.toString())
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    private fun hashString(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        val hexString = StringBuilder()
        for (byte in digest) {
            hexString.append(String.format("%02x", byte))
        }

        return hexString.toString()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.block_queue -> {
                val intent = Intent(requireContext(), BlockQueueActivity::class.java)
                startActivity(intent)
            }
            R.id.blockchain -> {
                val intent = Intent(requireContext(), BlockchainActivity::class.java)
                startActivity(intent)
            }
            R.id.transaction -> {
                val intent = Intent(requireContext(), MinerTransactionActivity::class.java)
                startActivity(intent)
            }
            R.id.inbox -> {
                val intent = Intent(requireContext(), InboxActivity::class.java)
                startActivity(intent)
            }
            R.id.rejected -> {
                val intent = Intent(requireContext(), RejectedBlocksActivity::class.java)
                startActivity(intent)
            }
            R.id.notifications -> {
                val intent = Intent(requireContext(), NotificationActivity::class.java)
                startActivity(intent)
            }
            R.id.account -> {
                val intent = Intent(requireContext(), AccountActivity::class.java)
                startActivity(intent)
            }
            R.id.logout -> {
                val sharedPrefs = context?.getSharedPreferences(SignInActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val editor = sharedPrefs?.edit()
                editor?.putBoolean("hasSignedIn", false)
                editor?.apply()

                val intent = Intent(requireContext(), SignInActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
        return true
    }
}