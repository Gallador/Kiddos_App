package com.dodolz.kiddos.kidsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener{
                val userProfile = userProfileChangeRequest{
                    displayName = namaLengkap
                }
                it.user?.updateProfile(userProfile)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) { createChildDocOnFirestore(email, namaLengkap) }
                }
            }
            .addOnFailureListener{
                _isSignupSuccessful.postValue(false)
                _signupFailedException.postValue(it)
            }
    }
    
    private fun createChildDocOnFirestore(email: String, namaLengkap: String)
        = viewModelScope.launch(Dispatchers.IO) {
        val user = hashMapOf(
            "email" to email,
            "nama" to namaLengkap,
            "status" to "anak",
            "kodeVerifikasi" to ""
        )
        val db = Firebase.firestore
        val userSpecificDocument = db.collection("User").document(email)
        val task1 = userSpecificDocument.set(user)
        Tasks.await(task1)
        userSpecificDocument.collection("Pengaturan").document("Perekaman")
            .set(hashMapOf("durasiPerekaman" to 1))
            .addOnSuccessListener { _isSignupSuccessful.postValue(true) }
    }
}