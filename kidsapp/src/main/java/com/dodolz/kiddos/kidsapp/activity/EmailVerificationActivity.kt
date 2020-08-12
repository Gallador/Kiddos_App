package com.dodolz.kiddos.kidsapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.dodolz.kiddos.kidsapp.HomeActivity
import com.dodolz.kiddos.kidsapp.R
import com.dodolz.kiddos.kidsapp.viewmodel.EmailVerificationViewmodel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_email_verification.*

class EmailVerificationActivity : AppCompatActivity() {
    
    private lateinit var viewmodel: EmailVerificationViewmodel
    private var currentUser: FirebaseUser? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)
        currentUser = FirebaseAuth.getInstance().currentUser
        viewmodel = ViewModelProvider(this).get(EmailVerificationViewmodel::class.java)
        viewmodel.isEmailHasBeenSent.observe(this, emailHasBeenSent)
        viewmodel.isUserVerified.observe(this, userVerified)
    }
    
    private val emailHasBeenSent = Observer<Pair<String, Boolean>> {
        val email = it.first
        val isEmailHasBeenSent = it.second
        if (isEmailHasBeenSent) {
            container_emailTerkirim.visibility = View.VISIBLE
            txt_email.text = email
        }
    }
    
    private val userVerified = Observer<Boolean> { isUserVerified ->
        if (isUserVerified) {
            startActivity(Intent(this, HomeActivity::class.java))
            this@EmailVerificationActivity.finishAndRemoveTask()
        }
    }
    
    override fun onStart() {
        super.onStart()
        viewmodel.checkIfEmailVerified()
    }
}