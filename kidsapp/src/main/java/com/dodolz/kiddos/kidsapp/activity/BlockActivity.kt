package com.dodolz.kiddos.kidsapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dodolz.kiddos.kidsapp.R

class BlockActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)
    }
    
    override fun onPause() {
        super.onPause()
        finish()
    }
    
    // For disable back button
    override fun onBackPressed() {
    
    }
}