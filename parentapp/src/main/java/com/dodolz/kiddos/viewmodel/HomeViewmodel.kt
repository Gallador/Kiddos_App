package com.dodolz.kiddos.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dodolz.kiddos.model.home.ChildInfo
import com.dodolz.kiddos.model.home.RecentApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewmodel(private val user: FirebaseUser): ViewModel() {
    
    private val db: FirebaseFirestore = Firebase.firestore
    //--------------------------------MutableMap<ChildEmail, -||- >
    private var mapOfChildrenUsageSum: MutableMap<String, ChildInfo> = mutableMapOf()
    //--------------------------------MutableMap<ChildEmail, -||- >
    private var mapOfChildrenRecentApps: MutableMap<String, ArrayList<RecentApp>> = mutableMapOf()
    
    private val _listOfChild: MutableLiveData<MutableList<DocumentSnapshot>> by lazy {
        MutableLiveData<MutableList<DocumentSnapshot>>()
    }
    
    // The key is childEmail: <String> for access child directory in storage to load appIcon
    private val _recentApps: MutableLiveData<Pair<String, ArrayList<RecentApp>?>> by lazy {
        MutableLiveData<Pair<String, ArrayList<RecentApp>?>>()
    }
    
    private val _usageSum: MutableLiveData<ChildInfo> by lazy {
        MutableLiveData<ChildInfo>()
    }
    
    private val _firebaseException: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>()
    }
    
    val listOfChild: LiveData<MutableList<DocumentSnapshot>>
        get() = _listOfChild
    
    val usageSum: LiveData<ChildInfo>
        get() = _usageSum
    
    // The key is childEmail: String for access child directory in storage to load appIcon
    val recentApps: LiveData<Pair<String, ArrayList<RecentApp>?>>
        get() = _recentApps
    
    val firebaseException: LiveData<Exception>
        get() = _firebaseException

    // this function only inovoked when MainActivity onStart
    // and only used for generate children tab layout selector
    fun loadListOfChild() = viewModelScope.launch(Dispatchers.IO) {
        val userEmail = user.email
        userEmail?.let { email ->
            val userSpecificDocument = db.collection("User").document(email)
            userSpecificDocument.collection("Daftar Anak").get()
                .addOnSuccessListener { collection ->
                    collection?.documents?.let { listOfChildResult->
                        _listOfChild.postValue(listOfChildResult)
                    }
                }
                .addOnFailureListener { exception -> _firebaseException.postValue(exception) }
        }
    }
    
    fun loadChildrenUsageSum(childEmail: String, forceLoad: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        if (mapOfChildrenUsageSum[childEmail] == null || forceLoad) {
            Log.d("loadChildrenUsageSum", "Get data from internet")
            val childDocument = db.collection("User").document(childEmail)
            childDocument.get().addOnSuccessListener { documentSnapshot ->
                documentSnapshot.toObject<ChildInfo>()?.let {
                    _usageSum.postValue(it)
                    mapOfChildrenUsageSum[childEmail] = it
                }
            }
        } else {
            Log.d("loadChildrenUsageSum", "Already there")
            _usageSum.postValue(mapOfChildrenUsageSum[childEmail])
        }
    }
    
    fun loadChildrenRecentApp(childEmail: String, forceLoad: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        if (mapOfChildrenRecentApps[childEmail] == null || forceLoad) {
            Log.d("loadRecentApp", "Get data from internet")
            val result: ArrayList<RecentApp> = arrayListOf()
            val childDocument = db.collection("User").document(childEmail).collection("Riwayat Akses Aplikasi")
            childDocument
                .orderBy("waktuAkses", Query.Direction.DESCENDING)
                .limit(15L)
                .get()
                .addOnCompleteListener { task ->
                    task.result?.let {
                        for (document in it) {
                            result.add(document.toObject())
                        }
                        _recentApps.postValue(Pair(childEmail, result))
                        mapOfChildrenRecentApps[childEmail] = result
                }
                
            }
        } else {
            Log.d("loadChildrenInfo", "Already there")
            _recentApps.postValue(Pair(childEmail, mapOfChildrenRecentApps[childEmail]))
        }
    }
}