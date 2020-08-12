package com.dodolz.kiddos.kidsapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.dodolz.kiddos.kidsapp.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_forgot_password.*

class ForgotPasswordActivity : AppCompatActivity() {
    
    private lateinit var loadingDialog: MaterialDialog
    private lateinit var successDialog: MaterialDialog
    private lateinit var failDialog: MaterialDialog
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        loadingDialog = MaterialDialog(this)
            .title(text = "Mengirim Email...")
            .message(text = "Mohon tunggu")
            .icon(R.drawable.ic_loading)
        successDialog = MaterialDialog(this)
            .title(text = "Berhasil Terkirim")
            .cancelable(true)
            .message(text = "Email untuk mengatur ulang kata sandi berhasil dikirim. Silakan periksa email Anda")
            .icon(R.drawable.ic_round_check_circle_24)
            .positiveButton(null, "Tutup") {
                it.dismiss()
            }
        failDialog = MaterialDialog(this)
            .title(text = "Gagal Terkirim")
            .cancelable(true)
            .message(text = "Email untuk mengatur ulang kata sandi gagal terkirim. Periksa kembali email Anda atau coba beberapa saat lagi")
            .icon(R.drawable.ic_baseline_cancel_24)
            .positiveButton(null, "Tutup") {
                it.dismiss()
            }
        btn_back.setOnClickListener { finish() }
        btn_kirimEmail.setOnClickListener {
            val email = edt_email.text.toString()
            if (email.isNotBlank()) {
                loadingDialog.show()
                Firebase.auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener{
                        loadingDialog.dismiss()
                        successDialog.show()
                        edt_email.text.clear()
                    }
                    .addOnFailureListener {
                        loadingDialog.dismiss()
                        failDialog.show()
                    }
            }
        }
    }
}