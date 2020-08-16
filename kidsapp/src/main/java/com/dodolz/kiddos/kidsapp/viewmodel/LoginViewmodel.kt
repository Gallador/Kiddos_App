package com.dodolz.kiddos.kidsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewmodel: ViewModel() {
    
    private lateinit var firebaseAuth: FirebaseAuth
    
    private val _isLoginSuccessfull: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _loginFailedException: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>()
    }
    private val _userIsNotKid: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    
    val isLoginSuccessfull: LiveData<Boolean>
        get() = _isLoginSuccessfull
    val loginFailedException: LiveData<Exception>
        get() = _loginFailedException
    val userIsNotKid: LiveData<Boolean>
        get() = _userIsNotKid
    
    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            firebaseAuth = FirebaseAuth.getInstance()
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener{ checkUserStatus(email) }
                .addOnFailureListener{ _loginFailedException.postValue(it) }
        }
    }

    private fun checkUserStatus(email: String) {
        FirebaseFirestore.getInstance().collection("User").document(email).get()
            .addOnCompleteListener {
                if (it.result != null) {
                    val mStatus = it.result?.get("status")
                    mStatus?.let { status ->
                        when (status.toString()) {
                            "anak" -> _isLoginSuccessfull.postValue(true)
                            "orangtua" -> _userIsNotKid.postValue(true)
                        }
                    }
                }
            }
    }
}