package com.example.wisechoice

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MinerTransactionActivity : AppCompatActivity() {

    var st_phone: String? = null
    var miner_bottom: BottomNavigationView? = null

    var addToBlockFragment: AddToBlockFragment = AddToBlockFragment()
    var addTransactionFragment: AddTransactionFragment = AddTransactionFragment()
    var mineFragment: MineFragment = MineFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_miner_transaction)
        val intent1 = intent
        st_phone = intent1.getStringExtra("Account")

        miner_bottom = findViewById<BottomNavigationView>(R.id.miner_bottom)

        supportFragmentManager.beginTransaction().replace(R.id.m_frame, addTransactionFragment)
            .commit()

        miner_bottom?.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.m_add_transaction -> {
                    supportFragmentManager.beginTransaction().replace(R.id.m_frame, addTransactionFragment)
                        .commit()
                }
                R.id.m_add_to_block -> {
                    supportFragmentManager.beginTransaction().replace(R.id.m_frame, addToBlockFragment)
                        .commit()
                }
                R.id.m_mine -> {
                    supportFragmentManager.beginTransaction().replace(R.id.m_frame, mineFragment)
                        .commit()
                }
            }
            true
        }
    }

    companion object {
        const val SHARED_PREF_NAME = "myPref"
    }
}