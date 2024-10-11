package com.example.wisechoice

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
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

class NotificationAdapter(
    private val context: Context,
    private val ids: List<String>,
    private val messages: List<String>,
    private val notification_times: List<String>,
    private val statuses: List<String>,
    private val activities: List<String>,

    private var databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference()

) : RecyclerView.Adapter<NotificationAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view: View = LayoutInflater.from(context).inflate(R.layout.notification_card, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val sharedPreferences = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        if("${messages[position]}" == st_phone && "${activities[position]}" == "BlockDetailsActivity") {
            holder.message.text = "Your Block is Now in the Queue, " +
                    "Awaiting Its Fate in the Blockchain. " +
                    "Stay Tuned!"
        }

        if("${messages[position]}" != st_phone && "${activities[position]}" == "BlockDetailsActivity") {
            holder.message.text = "Block Queue Alert: " +
                    "Someone Has Mined a New Block! " +
                    "Ready to Explore?"
        }

        if("${messages[position]}" == st_phone && "${activities[position]}" == "BlockchainDetailsActivity") {
            holder.message.text = "Success! Your Block Is Now on the Blockchain." +
                    " Want to Take a Look?"
        }

        if("${messages[position]}" != st_phone && "${activities[position]}" == "BlockchainDetailsActivity") {
            holder.message.text = "Blockchain Update: " +
                    "A New Block is Now in the Chain." +
                    "! Curious to Check It Out?"
        }

        holder.notification_time.text = "${notification_times[position]}"

        if("${statuses[position]}" == "Unread") {
            holder.message.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.notification_time.setTextColor(ContextCompat.getColor(context, R.color.white))

            holder.notification_card.setBackgroundColor(ContextCompat.getColor(context, R.color.ash))
        }

        if("${statuses[position]}" == "Read") {
            holder.message.setTextColor(ContextCompat.getColor(context, R.color.olive))
            holder.notification_time.setTextColor(ContextCompat.getColor(context, R.color.olive))

            holder.notification_card.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
        }

        val idValue = ids[position]
        val activity = activities[position]

        holder.notification_card.setOnClickListener {
            val sharedPreferences = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
            val st_phone = sharedPreferences.getString("Account", "") ?: ""

            FirebaseDatabase.getInstance().getReference("miners").child(st_phone).child("notifications")
                .child(idValue).child("Status").setValue("Read")

            try {
                val className = "com.example.wisechoice.$activity"

                val intent = Intent(context, Class.forName(className))
                intent.putExtra("block_id", idValue)
                intent.putExtra("path", "main_blockchain")

                context.startActivity(intent)

            } catch (e: ClassNotFoundException) {
            }

        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var message: TextView = itemView.findViewById(R.id.message)
        var notification_time: TextView = itemView.findViewById(R.id.notification_time)
        var notification_card: CardView = itemView.findViewById(R.id.notification_card)
    }
}

class NotificationActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapterClass: NotificationAdapter

    private lateinit var databaseReference: DatabaseReference

    private val ids = mutableListOf<String>()
    private val messages = mutableListOf<String>()
    private val notification_times = mutableListOf<String>()
    private val statuses = mutableListOf<String>()
    private val activities = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        val sharedPreferences = this.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE)
        val st_phone = sharedPreferences.getString("Account", "") ?: ""

        databaseReference = FirebaseDatabase.getInstance().getReference("miners").child(st_phone)
            .child("notifications")

        recyclerView = findViewById(R.id.recycler)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapterClass = NotificationAdapter(
            this,
            ids,
            messages,
            notification_times,
            statuses,
            activities
        )
        recyclerView.adapter = adapterClass

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ids.clear()
                messages.clear()
                notification_times.clear()
                statuses.clear()
                activities.clear()

                for (dataSnapshot in snapshot.children) {

                    val id = dataSnapshot.child("Notification_ID").value.toString()
                    val message = dataSnapshot.child("Message").value.toString()
                    val notification_time = dataSnapshot.child("Notification_Time").value.toString()
                    val status = dataSnapshot.child("Status").value.toString()
                    val activity = dataSnapshot.child("Activity").value.toString()

                    ids.add(id)
                    messages.add(message)
                    notification_times.add(notification_time)
                    statuses.add(status)
                    activities.add(activity)
                }
                adapterClass.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}