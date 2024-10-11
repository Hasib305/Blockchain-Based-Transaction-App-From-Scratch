package com.example.wisechoice

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.values
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

class AddTransactionFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var receiverField: EditText
    private lateinit var amountField: EditText
    private lateinit var feesField: EditText
    private lateinit var timeText: TextView

    private lateinit var signatureButton: Button
    private lateinit var st_phone: String
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseReference: DatabaseReference
    private lateinit var senderPrivateKey: String
    var drawerLayout: DrawerLayout? = null
    var navigationView: NavigationView? = null
    var nView: View? = null

    var username: TextView? = null
    var phone: TextView? = null
    var photo: ImageView? = null
    var home_menu: ImageView? = null
    private lateinit var formattedDateTime: String
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_transaction, container, false)

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

        receiverField = view.findViewById(R.id.receiver)
        amountField = view.findViewById(R.id.amount)
        feesField = view.findViewById(R.id.fees)
        timeText = view.findViewById(R.id.time_text)

        signatureButton = view.findViewById(R.id.signatureButton)

        databaseReference = FirebaseDatabase.getInstance().getReference()

        signatureButton.setOnClickListener {
            fetchSignatureFromFirebase()
        }


        return view
    }

    private fun fetchSignatureFromFirebase() {
        val sharedPreferences = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        databaseReference.child("miners").child(st_phone)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.child("Private_Key").exists()) {
                        senderPrivateKey = snapshot.child("Private_Key").value.toString()
                        val st_receiver = receiverField.text.toString()
                        val st_amount = amountField.text.toString()
                        val st_fees = feesField.text.toString()
                        formattedDateTime = getFormattedDateTime()
                        val st_transactionData = "$st_receiver$st_amount$st_fees$formattedDateTime"

                        val signature = Base64.getEncoder().encodeToString(createSignature(senderPrivateKey, st_transactionData))
                        Toast.makeText(requireContext(), "Signature created successfully.", Toast.LENGTH_SHORT).show()
                        performTransaction(signature)



                    } else {
                        Toast.makeText(requireContext(), "No private key found for the user.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun performTransaction(signature :String) {
        val sharedPreferences = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        val st_receiver = receiverField.text.toString()
        val st_amount = amountField.text.toString().toDoubleOrNull() ?: 0.0
        val st_fees = feesField.text.toString().toDoubleOrNull() ?: 0.0
        val st_signature = signature

        val startTime = System.currentTimeMillis()

        if (st_receiver.isEmpty() || st_amount == 0.0 || st_fees == 0.0 || st_signature.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        val minersRef = databaseReference.child("miners")

        minersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val phoneNumbers = snapshot.children.mapNotNull { it.key }

                var receiverExists = false
                var senderBalance = 0.0

                for (childSnapshot in snapshot.children) {
                    val phone = childSnapshot.key.toString()

                    if (phone == st_receiver) {
                        receiverExists = true

                        val random1 = phoneNumbers.filter { it != st_phone }.random()
                        val random2 = phoneNumbers.filter { it != st_phone }.random()
                        val random3 = phoneNumbers.filter { it != st_phone }.random()
                        val random4 = phoneNumbers.filter { it != st_phone }.random()

                        val balance1 =
                            snapshot.child(random1).child("Users_Balance").child(st_phone)
                        val balance2 =
                            snapshot.child(random2).child("Users_Balance").child(st_phone)
                        val balance3 =
                            snapshot.child(random3).child("Users_Balance").child(st_phone)
                        val balance4 =
                            snapshot.child(random4).child("Users_Balance").child(st_phone)
                        val balance =
                            snapshot.child(st_phone).child("Users_Balance").child(st_phone)

                        if(balance1.value == balance2.value && balance2.value == balance3.value &&
                            balance3.value == balance4.value) {

                            if(balance.value != balance1.value) {
                                databaseReference.child("miners").child(st_phone).child("Users_Balance")
                                    .child(st_phone).setValue(balance1.value)

                                Toast.makeText(context,
                                    "Your Data Was Corrupted. We Have Fixed It. Please Try Again To Transact",
                                    Toast.LENGTH_SHORT).show()
                            }
                            else {
                                var  initialBalance= childSnapshot.child("Users_Balance").child(st_phone)
                                    .child("Initial").value.toString().toDoubleOrNull() ?: 0.0

                                var  sentBalance= childSnapshot.child("Users_Balance").child(st_phone)
                                    .child("Sent").value.toString().toDoubleOrNull() ?: 0.0

                                var minedBalance = 0.0

                                val minedAmountSnapshot = childSnapshot.child("Users_Balance")
                                    .child(st_phone).child("Mined_Amount")

                                for (minedAmountChildSnapshot in minedAmountSnapshot.children) {
                                    val childValue = minedAmountChildSnapshot.value
                                    if (childValue is Number) {
                                        minedBalance += childValue.toDouble()
                                    }
                                }

                                var feesBalance = 0.0

                                val feesAmountSnapshot = childSnapshot.child("Users_Balance")
                                    .child(st_phone).child("Fees_Amount")

                                for (feesAmountChildSnapshot in feesAmountSnapshot.children) {
                                    val childValue = feesAmountChildSnapshot.value
                                    if (childValue is Number) {
                                        feesBalance += childValue.toDouble()
                                    }
                                }

                                var receivedBalance = 0.0

                                val receivedAmountSnapshot = childSnapshot.child("Users_Balance")
                                    .child(st_phone).child("Received_Amount")

                                for (receivedAmountChildSnapshot in receivedAmountSnapshot.children) {
                                    val childValue = receivedAmountChildSnapshot.value
                                    if (childValue is Number) {
                                        receivedBalance += childValue.toDouble()
                                    }
                                }

                                senderBalance = initialBalance + minedBalance + feesBalance + receivedBalance - sentBalance

                                if (st_amount + st_fees > senderBalance) {

                                    Toast.makeText(requireContext(), "Your Account's Balance is " + senderBalance +
                                            ". Insufficient to Perform This Transaction.",
                                        Toast.LENGTH_SHORT).show()
                                    return
                                }

                                val newBalance = st_amount + st_fees

                                updateSenderBalance(newBalance, st_amount, st_fees, st_receiver, st_signature,formattedDateTime)

                                val endTime = System.currentTimeMillis()
                                val timeRequiredForMining = endTime - startTime

                                timeText.text = "Time Required: $timeRequiredForMining ms"
                            }
                        }

                        else {
                            Toast.makeText(context,
                                "Someone Has Corrupted Data. Please Try Again To Transact", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                if (!receiverExists) {
                    Toast.makeText(requireContext(), "Receiver does not exist.", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun updateSenderBalance(newBalance: Double, amount: Double, fees: Double, receiver: String, signature: String,
                                    formattedDateTime: String) {
        val sharedPreferences = requireContext().getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        val senderRef = databaseReference.child("miners")

        val transactionKey = databaseReference.child("transactions").push().key

        senderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (childSnapshot in snapshot.children) {
                    val phone = childSnapshot.key

                    if (phone != null) {
                        senderRef.child(phone).child("Users_Balance").child(st_phone)
                            .child("Sent")

                        senderRef.child(phone).child("Users_Balance").child(st_phone)
                            .child("Sent").addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    val prevBalance = snapshot.getValue()
                                    val totalBalance = prevBalance.toString().toFloat() + newBalance.toFloat()
                                    senderRef.child(phone).child("Users_Balance").child(st_phone)
                                        .child("Sent").setValue(totalBalance)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    TODO("Not yet implemented")
                                }

                            })

                        val newTransactionRef = senderRef.child(phone).child("transactions").child(transactionKey!!)
                        val inboxRef = senderRef.child(st_phone).child("inbox").child(transactionKey!!)
                        val refString = newTransactionRef.key

                        inboxRef.apply {
                            child("Amount").setValue(amount)
                            child("Fees").setValue(fees)
                            child("Receiver").setValue(receiver)
                            child("Sender").setValue(st_phone)
                            child("Signature").setValue(signature)
                            child("Transaction_ID").setValue(refString.toString())
                            child("Transaction_Time").setValue(formattedDateTime)
                            child("Status").setValue("Unrecognized")
                        }

                        newTransactionRef.apply {
                            child("Amount").setValue(amount)
                            child("Fees").setValue(fees)
                            child("Receiver").setValue(receiver)
                            child("Sender").setValue(st_phone)
                            child("Signature").setValue(signature)
                            child("Transaction_ID").setValue(refString.toString())
                            child("Transaction_Time").setValue(formattedDateTime)
                            child("Status").setValue("Unrecognized")

                            Toast.makeText(requireContext(), "The Transaction Has Occurred.", Toast.LENGTH_SHORT).show()
                        }

                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        receiverField.text.clear()
        amountField.text.clear()
        feesField.text.clear()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createSignature(senderPrivateKey: String, dataToSign: String): ByteArray {
        val privateBytes = Base64.getDecoder().decode(senderPrivateKey)
        Log.d("SignatureVerification", "data: $dataToSign")

        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(privateBytes))

        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(dataToSign.toByteArray())

        return signature.sign()
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getFormattedDateTime(): String {
        val currentDateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return currentDateTime.format(formatter)
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

    companion object {
        const val PREFS_NAME = "MySharedPref"
    }
}