package com.dodolz.kiddos.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.dodolz.kiddos.MainActivity
import com.dodolz.kiddos.R
import com.dodolz.kiddos.viewmodel.EmailVerificationViewmodel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_email_verification.*

class EmailVerificationActivity : AppCompatActivity() {
    
    private lateinit var viewmodel: EmailVerificationViewmodel
    
    private val emailHasBeenSent = Observer<Boolean> { isEmailHasBeenSent ->
        if (isEmailHasBeenSent) {
            container_emailTerkirim.visibility = View.VISIBLE
            currentUser?.email?.let {
                txt_email.text = it
            }
        }
    }
    
    private val userVerified = Observer<Boolean> { isUserVerified ->
        if (isUserVerified) {
            startActivity(Intent(this, MainActivity::class.java))
            finishAndRemoveTask()
        }
    }
    
    private var currentUser: FirebaseUser? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)
        currentUser = Firebase.auth.currentUser
        viewmodel = ViewModelProvider(this).get(EmailVerificationViewmodel::class.java)
        viewmodel.isEmailHasBeenSent.observe(this, emailHasBeenSent)
        viewmodel.isUserVerified.observe(this, userVerified)
    }
    
    override fun onStart() {
        super.onStart()
        viewmodel.checkIfEmailVerified()
    }
}