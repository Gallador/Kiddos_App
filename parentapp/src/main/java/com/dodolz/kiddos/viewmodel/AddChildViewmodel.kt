package com.dodolz.kiddos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

internal class EmailAnak{
    var daftarAnak: List<String>? = null
}

class AddChildViewmodel: ViewModel() {
    
    private val db: FirebaseFirestore = Firebase.firestore
    private val userEmail: String? = FirebaseAuth.getInstance().currentUser?.email
    private var codeVerification: String? = null
    private var listOfChild: MutableList<String> = mutableListOf()
    private var emailAndNameChild: Pair<String, String> = Pair("", "")
    
    private val _isChildAlreadyAdded: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _isChildExist: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _codeVerificationHasBeenSent: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _isProcessSuccess: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    
    val isChildAlreadyAdded: LiveData<Boolean>
        get() = _isChildAlreadyAdded
    val isChildAccountExist: LiveData<Boolean>
        get() = _isChildExist
    val codeVerificationHasBeenSent: LiveData<Boolean>
        get() = _codeVerificationHasBeenSent
    val isProcessSuccess: LiveData<Boolean>
        get() = _isProcessSuccess
    
    // will invoked when user click "Tambahkan" button
    fun checkChildEmailFirst(childEmail: String) = viewModelScope.launch(Dispatchers.IO) {
        checkChildExist(childEmail)
    }
    
    private fun checkChildExist(childEmail: String) {
        val childSpecificDocument = db.collection("User").document(childEmail)
        childSpecificDocument.get()
            .addOnSuccessListener {
                if (it.get("email").toString() == childEmail) {
                    emailAndNameChild = Pair(
                        it.get("email").toString(),
                        it.get("nama").toString()
                    )
                    checkChildAlreadyAdded(childEmail)
                }
                else _isChildExist.postValue(false)
            }
            .addOnFailureListener{ _isChildExist.postValue(false) }
    }
    
    private fun checkChildAlreadyAdded(childEmail: String) {
        userEmail?.let { userEmail ->
            val userSpecificDocument = db.collection("User").document(userEmail)
            userSpecificDocument.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val childEmails = document.toObject<EmailAnak>()
                        childEmails?.daftarAnak?.let {
                            if (!it.contains(childEmail)) {
                                listOfChild.addAll(it)
                                sendCodeVerificationToChild(childEmail)
                            }
                            else
                                _isChildAlreadyAdded.postValue(true)
                        }
                    }
                }
                .addOnFailureListener {
                    _isChildAlreadyAdded.postValue(true)
                }
        }
    }
    
    private fun sendCodeVerificationToChild(childEmail: String) {
        val childSpecificDocument = db.collection("User").document(childEmail)
        val randomValues = List(6) { Random.nextInt(0, 10) }
        this@AddChildViewmodel.codeVerification = randomValues.joinToString("")
        childSpecificDocument.update("kodeVerifikasi", this@AddChildViewmodel.codeVerification)
            .addOnSuccessListener { _codeVerificationHasBeenSent.postValue(true) }
            .addOnFailureListener { _codeVerificationHasBeenSent.postValue(false) }
    }
    
    fun processAddChild(codeFromUser: String) = viewModelScope.launch(Dispatchers.IO) {
        if (codeFromUser == this@AddChildViewmodel.codeVerification) {
            val childEmail = emailAndNameChild.first
            val childName = emailAndNameChild.second
            
            // Update field daftarAnak di db ortu
            userEmail?.let {
                val userSpecificDocument = db.collection("User").document(userEmail)
                listOfChild.add(childEmail)
                userSpecificDocument
                    .update("daftarAnak", listOfChild).addOnSuccessListener {
                        
                        // Tambahkan collection Daftar Anak di db ortu
                        userSpecificDocument.collection("Daftar Anak").document(childEmail)
                            .set(hashMapOf("email" to childEmail, "nama" to childName)).addOnSuccessListener {
    
                                // Update kodeVerifikasi di db anak kembali ke ""
                                val childSpecificDocument = db.collection("User").document(childEmail)
                                childSpecificDocument.update("kodeVerifikasi", "")
                                    .addOnSuccessListener { _isProcessSuccess.postValue(true) }
                                    .addOnFailureListener{ _isProcessSuccess.postValue(false) }
                            }
                            .addOnFailureListener{ _isProcessSuccess.postValue(false) }
                    }
                    .addOnFailureListener{ _isProcessSuccess.postValue(false) }
            }
        }
    }
}