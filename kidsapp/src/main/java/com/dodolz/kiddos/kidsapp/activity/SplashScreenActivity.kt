package com.dodolz.kiddos.kidsapp.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dodolz.kiddos.kidsapp.HomeActivity
import com.dodolz.kiddos.kidsapp.R
import com.google.firebase.auth.FirebaseAuth

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        val mAuth = FirebaseAuth.getInstance()
        val user = mAuth.currentUser
        if (user != null) {
            val intent: Intent = when {
                user.isEmailVerified -> Intent(this, HomeActivity::class.java)
                else -> Intent(this, EmailVerificationActivity::class.java)
            }
            startActivity(intent)
            finish()
        } else {
            Intent(this, LoginActivity::class.java).also {
                startActivity(it)
                finish()
            }
        }
    }
}