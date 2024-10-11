package com.example.wisechoice
import android.util.Log
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
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

class TempBlockAdapter(
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

) : RecyclerView.Adapter<TempBlockAdapter.MyViewHolder>() {

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

        checkBlockchain("main_blockchain")
        checkBlockchain("blockchain")

        if(verifies[position] == "Unrecognized") {
            holder.sender.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.receiver.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.fees.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.verify.setTextColor(ContextCompat.getColor(context, R.color.olive))

            holder.verify_button.isEnabled = true
            holder.verify_button.text = "Verify"
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }

        else if(verifies[position] == "Verified") {
            holder.sender.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.receiver.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.fees.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.verify.setTextColor(ContextCompat.getColor(context, R.color.olive))

            holder.verify_button.isEnabled = true
            holder.verify_button.text = "Add To Block"
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
        }

        else if(verifies[position] == "Not Verified") {
            holder.sender.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.receiver.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.fees.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.verify.setTextColor(ContextCompat.getColor(context, R.color.olive))

            holder.verify_button.isEnabled = false
            holder.verify_button.text = "Denied"
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_green ))
        }

        else if(verifies[position] == "In Processing...") {
            holder.sender.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.receiver.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.fees.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.verify.setTextColor(ContextCompat.getColor(context, R.color.olive))

            holder.verify_button.isEnabled = false
            holder.verify_button.text = "Wait"
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.orange))
        }

        else if(verifies[position] == "Temporary Blocked") {
            holder.sender.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.receiver.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.fees.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.verify.setTextColor(ContextCompat.getColor(context, R.color.white))

            holder.verify_button.isEnabled = false
            holder.verify_button.text = "Wait"
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.ash))
        }

        else if(verifies[position] == "Blocked") {
            holder.sender.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.receiver.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.amount.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.fees.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.verify.setTextColor(ContextCompat.getColor(context, R.color.white))

            holder.verify_button.isEnabled = false
            holder.verify_button.text = "Done"
            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.olive))
        }

        holder.verify_button.setOnClickListener {
            val startTime = System.currentTimeMillis()

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
                                .child("transactions").child(idValue)
                            newTransactionRef.child("Status").setValue("Verified")

                            holder.verify.text = "Verified"
                            holder.verify_button.text = "Add To Block"
                            holder.verify_button.isEnabled = true
                            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.green))
                        }

                        else {
                            val newTransactionRef = databaseReference.child("miners").child(st_phone)
                                .child("transactions").child(idValue)
                            newTransactionRef.child("Status").setValue("Not Verified")

                            holder.verify.text = "Not Verified"
                            holder.verify_button.text = "Denied"
                            holder.verify_button.isEnabled = false
                            holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.dark_green))
                        }

                        val endTime = System.currentTimeMillis()
                        val timeRequiredForMining = endTime - startTime

                        holder.verify_button.text = "$timeRequiredForMining ms"
                    }

                }
            }

            else if(holder.verify.text == "Verified") {
                val newTransactionRef = databaseReference.child("miners").child(st_phone)
                    .child("transactions").child(idValue)
                newTransactionRef.child("Status").setValue("In Processing...")

                holder.verify.text = ""
                holder.verify_button.text = "In Processing..."
                holder.verify_button.isEnabled = false
                holder.transaction_card.setBackgroundColor(ContextCompat.getColor(context, R.color.yellow))

                val tempTransactionRef = databaseReference.child("miners").child(st_phone)
                    .child("temporary_blocks").child(idValue)

                tempTransactionRef.child("Transaction_ID").setValue("${ids[position]}")
                tempTransactionRef.child("Sender").setValue("${senders[position]}")
                tempTransactionRef.child("Receiver").setValue("${receivers[position]}")
                tempTransactionRef.child("Amount").setValue("${amounts[position]}")
                tempTransactionRef.child("Fees").setValue("${feeses[position]}")
                tempTransactionRef.child("Signature").setValue("${signatures[position]}")
                tempTransactionRef.child("Transaction_Time").setValue("${transaction_times[position]}")
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

    private fun checkBlockchain(path: String) {
        val sharedPreferences = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
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

                            if(path == "main_blockchain") {
                                FirebaseDatabase.getInstance()
                                    .getReference("miners")
                                    .child(st_phone)
                                    .child("transactions")
                                    .child(transactionKey.toString())
                                    .child("Block_No")
                                    .setValue(blockKey.toString())
                            }

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
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
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

class AddToBlockFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterClass: TempBlockAdapter
    private lateinit var statusText: TextView
    private lateinit var statusCard: CardView

    private lateinit var databaseReference: DatabaseReference

    private val senders = mutableListOf<String>()
    private val receivers = mutableListOf<String>()
    private val amounts = mutableListOf<String>()
    private val feeses = mutableListOf<String>()
    private val verifies = mutableListOf<String>()
    private val ids = mutableListOf<String>()
    private val signatures = mutableListOf<String>()
    private val transaction_times = mutableListOf<String>()

    var drawerLayout: DrawerLayout? = null
    var navigationView: NavigationView? = null
    var nView: View? = null

    var username: TextView? = null
    var phone: TextView? = null
    var photo: ImageView? = null
    var home_menu: ImageView? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_to_block, container, false)
    }

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

        var sharedPreferences =
            requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        var st_phone = sharedPreferences.getString("Account", "") ?: ""

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

        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child("transactions")

        recyclerView = view.findViewById(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapterClass = TempBlockAdapter(
            requireContext(),
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

        statusSelected("Unrecognized", "Verified", "Unrecognized",
            "Verified", "Unrecognized", "Verified")

        statusText = view.findViewById(R.id.status_text)
        statusCard = view.findViewById(R.id.status_card)

        statusCard.setOnClickListener { showPopupMenu(statusCard) }
    }

    private fun showPopupMenu(statusCard: CardView?) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.status_options)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.unblocked -> {
                    statusText.text = "Unblocked"
                    statusSelected("Unrecognized", "Verified", "Unrecognized",
                        "Verified", "Unrecognized", "Verified")
                    true
                }
                R.id.not_verified -> {
                    statusText.text = "Not Verified"
                    statusSelected("Not Verified", "Not Verified", "Not Verified",
                        "Not Verified", "Not Verified", "Not Verified")
                    true
                }
                R.id.processing -> {
                    statusText.text = "In Processing..."
                    statusSelected("In Processing...", "In Processing...", "In Processing...",
                        "In Processing...", "In Processing...", "In Processing...")
                    true
                }
                R.id.temporary_blocked -> {
                    statusText.text = "Temporary Blocked"
                    statusSelected("Temporary Blocked", "Temporary Blocked", "Temporary Blocked",
                        "Temporary Blocked", "Temporary Blocked", "Temporary Blocked")
                    true
                }
                R.id.blocked -> {
                    statusText.text = "Blocked"
                    statusSelected("Blocked", "Blocked", "Blocked",
                        "Blocked", "Blocked", "Blocked")
                    true
                }
                R.id.all -> {
                    statusText.text = "All Transactions"
                    statusSelected("Unrecognized", "Verified", "Not Verified",
                        "In Processing...", "Temporary Blocked","Blocked")
                    true
                }
                else -> false
            }
        }

        popupMenu.show()
    }

    fun statusSelected(status1: String, status2: String, status3: String, status4: String, status5: String, status6: String) {
        val sharedPreferences = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

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
                    val verify = dataSnapshot.child("Status").value.toString()

                    if(verify == status1 || verify == status2 || verify == status3 || verify == status4
                        || verify == status5 || verify == status6) {

                        val sender = dataSnapshot.child("Sender").value.toString()
                        val receiver = dataSnapshot.child("Receiver").value.toString()
                        val amount = dataSnapshot.child("Amount").value.toString()
                        val fees = dataSnapshot.child("Fees").value.toString()
                        val signature = dataSnapshot.child("Signature").value.toString()
                        val transaction_time =
                            dataSnapshot.child("Transaction_Time").value.toString()
                        val id = dataSnapshot.child("Transaction_ID").value.toString()

                        senders.add(sender)
                        receivers.add(receiver)
                        amounts.add(amount)
                        feeses.add(fees)
                        verifies.add(verify)
                        ids.add(id)
                        signatures.add(signature)
                        transaction_times.add(transaction_time)
                    }
                }
                adapterClass.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
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