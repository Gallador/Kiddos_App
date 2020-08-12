package com.dodolz.kiddos.kidsapp.activity

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import com.dodolz.kiddos.kidsapp.R
import com.dodolz.kiddos.kidsapp.viewmodel.SignupViewmodel
import kotlinx.android.synthetic.main.activity_signup.*

class SignupActivity : AppCompatActivity(), View.OnClickListener {
    
    private lateinit var viewmodel: SignupViewmodel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        viewmodel = ViewModelProvider(this).get(SignupViewmodel::class.java)
        viewmodel.isSignupSuccessful.observe(this){ isSignupSuccessful ->
            if (isSignupSuccessful) {
                startActivity(Intent(this, EmailVerificationActivity::class.java))
                finish()
            }
        }
        viewmodel.signupFailedException.observe(this) { showCauseOfSignupFailure(it) }
        lbl_masuk.setOnClickListener(this)
        showPassword.setOnCheckedChangeListener { _, isChecked ->
            edt_kataSandi.inputType =
                if (!isChecked) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
            edt_kataSandi.transformationMethod =
                if (!isChecked) PasswordTransformationMethod.getInstance() else null
            edt_ulangiKataSandi.inputType =
                if (!isChecked) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
            edt_ulangiKataSandi.transformationMethod =
                if (!isChecked) PasswordTransformationMethod.getInstance() else null
        }
        btn_daftar.setOnClickListener(this)
        btn_back.setOnClickListener(this)
    }
    
    private fun showCauseOfSignupFailure(exception: Exception) {
        edt_email.text.clear()
        edt_kataSandi.text.clear()
        edt_ulangiKataSandi.text.clear()
        container.visibility = View.VISIBLE
        Toast.makeText(
            this,
            exception.message.toString(),
            Toast.LENGTH_LONG
        ).show()
    }
    
    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_daftar -> {
                val name = edt_namaLengkap.text.toString()
                val email = edt_email.text.toString()
                val pass = edt_kataSandi.text.toString()
                val repeatPass = edt_ulangiKataSandi.text.toString()
                if (pass != repeatPass) {
                    Toast.makeText(this, "Kata Sandi tidak sama", Toast.LENGTH_LONG).show()
                    edt_kataSandi.text.clear()
                    edt_ulangiKataSandi.text.clear()
                }
                else if (name.isBlank() || email.isBlank() || pass.isBlank() || repeatPass.isBlank()) {
                    Toast.makeText(this, "Semua formulir harus diisi", Toast.LENGTH_LONG).show()
                }
                else if (email.isNotBlank() && pass.isNotBlank() && name.isNotBlank()) {
                    container.visibility = View.INVISIBLE
                    viewmodel.signup(email, pass, name)
                }
            }
            R.id.lbl_masuk -> finish()
            R.id.btn_back -> finish()
        }
    }
}