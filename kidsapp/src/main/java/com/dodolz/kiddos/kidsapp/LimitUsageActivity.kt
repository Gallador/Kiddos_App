package com.dodolz.kiddos.kidsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LimitUsageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_limit_usage)
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    // For disable back button
    override fun onBackPressed() {

    }
}