package com.dodolz.kiddos.navigation

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.dodolz.kiddos.R
import com.dodolz.kiddos.viewmodel.AddChildViewmodel
import kotlinx.android.synthetic.main.activity_add_child.*

class AddChildActivity : AppCompatActivity() {
    
    private lateinit var viewmodel: AddChildViewmodel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_child)
        viewmodel = ViewModelProvider(this).get(AddChildViewmodel::class.java)
        subscribeObservers()
        btn_tambahAnak.setOnClickListener{
            if (edt_email.text.isNotBlank()) {
                viewmodel.checkChildEmailFirst(edt_email.text.toString())
                container_tambahAnak.visibility = View.INVISIBLE
            }
        }
        btn_verifikasi.setOnClickListener {
            if (edt_kodeUnik.text.isNotBlank()) {
                viewmodel.processAddChild(edt_kodeUnik.text.toString())
                container_verifikasiAnak.visibility = View.INVISIBLE
            }
        }
        btn_back.setOnClickListener{ finish() }
    }
    
    private fun subscribeObservers() {
        viewmodel.isChildAlreadyAdded.observe(this, Observer {
            if (it) {
                container_tambahAnak.visibility = View.VISIBLE
                Toast.makeText(this, "Anak sudah ditambahkan", Toast.LENGTH_LONG).show()
                edt_email.text.clear()
                edt_email.requestFocus()
            }
        })
        
        viewmodel.isChildAccountExist.observe(this, Observer {
            if (!it) {
                container_tambahAnak.visibility = View.VISIBLE
                Toast.makeText(this, "Akun anak belum terdatar", Toast.LENGTH_LONG).show()
                edt_email.text.clear()
                edt_email.requestFocus()
            }
        })
        
        viewmodel.codeVerificationHasBeenSent.observe(this, Observer {
            if (it) {
                container_verifikasiAnak.visibility = View.VISIBLE
                Toast.makeText(this, "Kode verifikasi berhasil dikirim ke Akun Anak", Toast.LENGTH_LONG).show()
                edt_kodeUnik.requestFocus()
            }
        })
        
        viewmodel.isProcessSuccess.observe(this, Observer {
            if (it) {
                Toast.makeText(this, "Anak Berhasil Ditambahkan", Toast.LENGTH_LONG).show()
                finish()
            }
        })
    }
    
    override fun onBackPressed() {
        finish()
    }
}