package com.dodolz.kiddos.kidsapp.activity

import android.graphics.Paint
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.DialogBehavior
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.ModalDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.dodolz.kiddos.kidsapp.R
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_edit_profile.*

class EditProfileActivity : AppCompatActivity() {
    
    private var changePassDialog: MaterialDialog? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        lbl_gantiKataSandi.paintFlags = lbl_gantiKataSandi.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        btn_back.setOnClickListener {
            finish()
        }
        Firebase.auth.currentUser?.let { user ->
            edt_namaLengkap.append(user.displayName.toString())
            lbl_gantiKataSandi.setOnClickListener {
                showCustomViewDialog(BottomSheet(LayoutMode.WRAP_CONTENT), user)
            }
            lbl_simpan.setOnClickListener {
                val editedName = edt_namaLengkap.text.toString()
                if (user.displayName.toString() != editedName && editedName.isNotEmpty()) {
                    container_main.visibility = View.INVISIBLE
                    container_loading.visibility = View.VISIBLE
                    val nameUpdates = userProfileChangeRequest {
                        displayName = editedName
                    }
                    user.updateProfile(nameUpdates)
                        .addOnSuccessListener {
                            Firebase.firestore.collection("User").document(user.email.toString())
                                .update("nama", editedName)
                                .addOnSuccessListener {
                                    container_main.visibility = View.VISIBLE
                                    container_loading.visibility = View.INVISIBLE
                                    Toast.makeText(this@EditProfileActivity, "Nama Lengkap berhasil diperbarui", Toast.LENGTH_LONG).show()
                                }
                        }
                }
            }
        }
    }
    
    private fun showCustomViewDialog(
        dialogBehavior: DialogBehavior = ModalDialog,
        user: FirebaseUser
    ) {
        changePassDialog = MaterialDialog(this, dialogBehavior).show {
            title(R.string.ganti_kata_sandi)
            cornerRadius(16f)
            noAutoDismiss()
            customView(R.layout.custom_view_change_password, scrollable = true, horizontalPadding = true)
            positiveButton(R.string.simpan) { dialog ->
                val passInput = dialog.getCustomView().findViewById<EditText>(R.id.edt_kataSandiSekarang)
                    .text.toString()
                val newPassInput = dialog.getCustomView().findViewById<EditText>(R.id.edt_kataSandiBaru)
                    .text.toString()
                val repeatNewPassInput = dialog.getCustomView().findViewById<EditText>(R.id.edt_ulangiKataSandi)
                    .text.toString()
                user.email?.let { email ->
                    dialog.getCustomView().findViewById<LinearLayout>(R.id.container_main)
                        .visibility = View.INVISIBLE
                    dialog.getCustomView().findViewById<LinearLayout>(R.id.container_loading)
                        .visibility = View.VISIBLE
                    val credential = EmailAuthProvider.getCredential(email, passInput)
                    fun changeUI() {
                        dialog.getCustomView().findViewById<LinearLayout>(R.id.container_main)
                            .visibility = View.VISIBLE
                        dialog.getCustomView().findViewById<LinearLayout>(R.id.container_loading)
                            .visibility = View.INVISIBLE
                    }
                    user.reauthenticate(credential)
                        .addOnSuccessListener {
                            if (newPassInput.isNotEmpty() && repeatNewPassInput.isNotEmpty() &&
                                (newPassInput == repeatNewPassInput)) {
                                user.updatePassword(newPassInput).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        dialog.dismiss()
                                        Toast.makeText(this@EditProfileActivity, "Kata Sandi berhasil diganti", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(this@EditProfileActivity, "Penggantian Kata Sandi gagal. Ulangi beberapa saat lagi", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                changeUI()
                                Toast.makeText(this@EditProfileActivity, "Periksa dan lengkapi kembali Kata Sandi yang dimasukkan", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener {
                            changeUI()
                            dialog.getCustomView().findViewById<EditText>(R.id.edt_kataSandiSekarang)
                                .text.clear()
                            dialog.getCustomView().findViewById<EditText>(R.id.edt_kataSandiSekarang)
                                .requestFocus()
                            Toast.makeText(this@EditProfileActivity, "Kata Sandi yang dimasukkan tidak cocok", Toast.LENGTH_LONG).show()
                        }
                }
            }
            negativeButton(R.string.batal) {
                it.dismiss()
            }
        }
        
        // Setup custom view content
        changePassDialog?.let {
            val customView = it.getCustomView()
            val newPassInput: EditText = customView.findViewById(R.id.edt_kataSandiBaru)
            val repeatNewPassInput: EditText = customView.findViewById(R.id.edt_ulangiKataSandi)
            val showPasswordCheck: CheckBox = customView.findViewById(R.id.showPassword)
            showPasswordCheck.setOnCheckedChangeListener { _, isChecked ->
                newPassInput.inputType =
                    if (!isChecked) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
                newPassInput.transformationMethod =
                    if (!isChecked) PasswordTransformationMethod.getInstance() else null
                repeatNewPassInput.inputType =
                    if (!isChecked) InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT
                repeatNewPassInput.transformationMethod =
                    if (!isChecked) PasswordTransformationMethod.getInstance() else null
            }
        }
    }

/*
positiveButton(R.string.simpan) { dialog ->
                val newPassInput = dialog.getCustomView().findViewById<EditText>(R.id.edt_kataSandiBaru)
                    .text.toString()
                val repeatNewPassInput = dialog.getCustomView().findViewById<EditText>(R.id.edt_ulangiKataSandi)
                    .text.toString()
                dialog.getCustomView().findViewById<LinearLayout>(R.id.container_main)
                    .visibility = View.INVISIBLE
                dialog.getCustomView().findViewById<LinearLayout>(R.id.container_loading)
                    .visibility = View.VISIBLE
                fun changeBackUI() {
                    dialog.getCustomView().findViewById<LinearLayout>(R.id.container_main)
                        .visibility = View.VISIBLE
                    dialog.getCustomView().findViewById<LinearLayout>(R.id.container_loading)
                        .visibility = View.INVISIBLE
                }
                if (newPassInput.isNotEmpty() && repeatNewPassInput.isNotEmpty() &&
                    (newPassInput == repeatNewPassInput)) {
                    user.updatePassword(newPassInput).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            dialog.dismiss()
                            Toast.makeText(this@EditProfileActivity, "Kata Sandi berhasil diganti", Toast.LENGTH_LONG).show()
                        } else {
                            changeBackUI()
                            Toast.makeText(this@EditProfileActivity, "Penggantian Kata Sandi gagal. Ulangi beberapa saat lagi", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    changeBackUI()
                    Toast.makeText(this@EditProfileActivity, "Kata Sandi yang dimasukkan tidak sama", Toast.LENGTH_LONG).show()
                }
            }

    
*/
}