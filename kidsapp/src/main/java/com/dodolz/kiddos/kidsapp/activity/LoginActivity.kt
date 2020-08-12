package com.dodolz.kiddos.kidsapp.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.dodolz.kiddos.kidsapp.HomeActivity
import com.dodolz.kiddos.kidsapp.R
import com.dodolz.kiddos.kidsapp.viewmodel.LoginViewmodel
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    
    private lateinit var viewmodel: LoginViewmodel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        viewmodel = ViewModelProvider(this).get(LoginViewmodel::class.java)
        viewmodel.isLoginSuccessfull.observe(this) { isLoginSuccessfull ->
            if (isLoginSuccessfull) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
        viewmodel.loginFailedException.observe(this) { showCauseOfLoginFailure(it) }
        btn_masuk.setOnClickListener(this)
        lbl_daftar.setOnClickListener(this)
        lbl_lupaKataSandi.setOnClickListener(this)
    }
    
    private fun showCauseOfLoginFailure(exception: Exception) {
        edt_email.text.clear()
        edt_email.requestFocus()
        edt_kataSandi.text.clear()
        container.visibility = View.VISIBLE
        Toast.makeText(
            this,
            exception.message.toString(),
            Toast.LENGTH_LONG
        ).show()
    }
    
    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_masuk -> {
                val email = edt_email.text.toString()
                val password = edt_kataSandi.text.toString()
                if (email.isNotBlank() && password.isNotBlank()) {
                    container.visibility = View.INVISIBLE
                    viewmodel.login(email, password)
                }
                else if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(this, "Semua informasi harus diisi", Toast.LENGTH_LONG).show()
                }
            }
            R.id.lbl_daftar -> startActivity(Intent(this, SignupActivity::class.java))
            R.id.lbl_lupaKataSandi -> startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}