package com.example.wisechoice

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.cardview.widget.CardView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest

class BlockchainAdapter(
    private val context: Context,
    private val ids: List<String>,
    private val miners: List<String>,
    private val no_of_transactionss: List<String>,
    private val total_sents: List<String>,
    private val mined_times: List<String>,

    private var databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference()

) : RecyclerView.Adapter<BlockchainAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.block, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        val sharedPreferences = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        if("${miners[position]}" != st_phone) {
            holder.miner.text = "${hashText(miners[position])}.."
        }
        else {
            holder.miner.text = "${miners[position]}"
        }
        holder.no_of_transactions.text = "${no_of_transactionss[position]}"
        holder.total_sent.text = "${total_sents[position]}"
        holder.mined_time.text = "${mined_times[position]}"

        val idValue = ids[position]

        holder.block_card.setOnClickListener {

            val intent = Intent(context, BlockchainDetailsActivity::class.java)

            intent.putExtra("block_id", idValue)
            intent.putExtra("path", "main_blockchain")
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
        return ids.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var miner: TextView = itemView.findViewById(R.id.miner)
        var no_of_transactions: TextView = itemView.findViewById(R.id.no_of_transactions)
        var total_sent: TextView = itemView.findViewById(R.id.total_sent)
        var mined_time: TextView = itemView.findViewById(R.id.mined_time)
        var block_card = itemView.findViewById<CardView>(R.id.block_card)
    }
}

class BlockchainActivity : AppCompatActivity() , NavigationView.OnNavigationItemSelectedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterClass: BlockchainAdapter

    private lateinit var databaseReference: DatabaseReference

    private val ids = mutableListOf<String>()
    private val miners = mutableListOf<String>()
    private val no_of_transactionss = mutableListOf<String>()
    private val total_sents = mutableListOf<String>()
    private val mined_times = mutableListOf<String>()

    var drawerLayout: DrawerLayout? = null
    var navigationView: NavigationView? = null
    var nView: View? = null

    var username: TextView? = null
    var phone: TextView? = null
    var photo: ImageView? = null
    var home_menu: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blockchain)

        drawerLayout = findViewById<DrawerLayout>(R.id.drawer)

        val actionBarDrawerToggle = ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        actionBarDrawerToggle.syncState()

        navigationView = findViewById<NavigationView>(R.id.navigation)
        nView = navigationView?.getHeaderView(0)
        username = nView?.findViewById<TextView>(R.id.username)
        phone = nView?.findViewById<TextView>(R.id.phone)
        home_menu = findViewById<ImageView>(R.id.home_menu)

        home_menu?.setOnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }

        navigationView?.setNavigationItemSelectedListener(this)

        val sharedPreferences = this.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
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

        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child("main_blockchain")

        recyclerView = findViewById(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapterClass = BlockchainAdapter(
            this,
            ids,
            miners,
            no_of_transactionss,
            total_sents,
            mined_times
        )
        recyclerView.adapter = adapterClass

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ids.clear()
                miners.clear()
                no_of_transactionss.clear()
                total_sents.clear()
                mined_times.clear()

                val reversedSnapshot = snapshot.children.reversed()

                for (dataSnapshot in reversedSnapshot) {
                    val id = dataSnapshot.child("Block_ID").value.toString()
                    val miner = dataSnapshot.child("Miner").value.toString()
                    val transaction = dataSnapshot.child("No_Of_Transactions").value.toString()
                    val sent = dataSnapshot.child("Total_Amount").value.toString()
                    val time = dataSnapshot.child("Mined_Time").value.toString()

                    ids.add(id)
                    miners.add(miner)
                    no_of_transactionss.add(transaction)
                    total_sents.add(sent)
                    mined_times.add(time)
                }
                adapterClass.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.block_queue -> {
                val intent = Intent(this, BlockQueueActivity::class.java)
                startActivity(intent)
            }
            R.id.blockchain -> {
                val intent = Intent(this, BlockchainActivity::class.java)
                startActivity(intent)
            }
            R.id.transaction -> {
                val intent = Intent(this, MinerTransactionActivity::class.java)
                startActivity(intent)
            }
            R.id.inbox -> {
                val intent = Intent(this, InboxActivity::class.java)
                startActivity(intent)
            }
            R.id.rejected -> {
                val intent = Intent(this, RejectedBlocksActivity::class.java)
                startActivity(intent)
            }
            R.id.notifications -> {
                val intent = Intent(this, NotificationActivity::class.java)
                startActivity(intent)
            }
            R.id.account -> {
                val intent = Intent(this, AccountActivity::class.java)
                startActivity(intent)
            }
            R.id.logout -> {
                val sharedPrefs = this.getSharedPreferences(SignInActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val editor = sharedPrefs?.edit()
                editor?.putBoolean("hasSignedIn", false)
                editor?.apply()

                val intent = Intent(this, SignInActivity::class.java)
                startActivity(intent)
                this.finish()
            }
        }
        return true
    }
}