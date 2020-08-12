package com.dodolz.kiddos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignupViewmodel: ViewModel() {
    
    private lateinit var firebaseAuth: FirebaseAuth
    
    private val _isSignupSuccessful: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _signupFailedException: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>()
    }
    
    val isSignupSuccessful: LiveData<Boolean>
        get() = _isSignupSuccessful
    val signupFailedException: LiveData<Exception>
        get() = _signupFailedException
    
    fun signup(email: String, password: String, namaLengkap: String) {
        firebaseAuth = Firebase.auth
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener{
                val userProfile = userProfileChangeRequest{
                    displayName = namaLengkap
                }
                it.user?.updateProfile(userProfile)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) { createParentDocOnFirestore(email, namaLengkap) }
                }
            }
            .addOnFailureListener{
                _isSignupSuccessful.postValue(false)
                _signupFailedException.postValue(it)
            }
    }
    
    private fun createParentDocOnFirestore(email: String, namaLengkap: String) {
        val user = hashMapOf(
            "email" to email,
            "nama" to namaLengkap,
            "status" to "orangtua",
            "daftarAnak" to arrayListOf<String>()
        )
        val db = Firebase.firestore
        val userSpecificDocument = db.collection("User").document(email)
        userSpecificDocument.set(user)
            .addOnSuccessListener { _isSignupSuccessful.postValue(true) }
    }
}