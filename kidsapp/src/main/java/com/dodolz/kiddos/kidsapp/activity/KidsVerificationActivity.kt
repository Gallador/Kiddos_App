package com.dodolz.kiddos.kidsapp.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.dodolz.kiddos.kidsapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_kids_verification.*

class KidsVerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kids_verification)
        btn_back.setOnClickListener{
            finish()
        }
        FirebaseAuth.getInstance().currentUser?.let { user ->
            Firebase.firestore.collection("User").document(user.email.toString()).get()
                .addOnSuccessListener {
                    it.get("kodeVerifikasi")?.let { kode ->
                        if (kode.toString().isNotEmpty()) {
                            txt_kodeUnik.visibility = View.VISIBLE
                            txt_kodeUnik.text = kode.toString()
                        } else {
                            lbl_belumTersedia.visibility = View.VISIBLE
                        }
                    }
                }
        }
    }
}