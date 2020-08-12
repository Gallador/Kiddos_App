package com.dodolz.kiddos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.dodolz.kiddos.model.detail.DetailApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class AppDetailViewmodel: ViewModel() {
    
    private val db: FirebaseFirestore = Firebase.firestore
    private var childrenDetailApp: MutableMap<String, MutableList<DetailApp>> = mutableMapOf()
    
    private val _listDetailApps: MutableLiveData<Pair<String, MutableList<DetailApp>>> by lazy {
        MutableLiveData<Pair<String, MutableList<DetailApp>>>()
    }
    private val _changeDetailApps: MutableLiveData<Pair<String, MutableList<DetailApp>>> by lazy {
        MutableLiveData<Pair<String, MutableList<DetailApp>>>()
    }
    
    val listDetailApps: LiveData<Pair<String, MutableList<DetailApp>>>
        get() = _listDetailApps
    val changeDetailApps: LiveData<Pair<String, MutableList<DetailApp>>>
        get() = _changeDetailApps
    
    fun loadDetailApps(childEmail: String) {
        if (childrenDetailApp[childEmail] == null) {
            val result: MutableList<DetailApp> = mutableListOf()
            db.collection("User").document(childEmail).collection("Detail Penggunaan")
                .orderBy("durasiPenggunaan", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener { task ->
                    task.result?.let {
                        for (document in it) {
                            result.add(document.toObject())
                        }
                        _listDetailApps.postValue(Pair(childEmail, result))
                        childrenDetailApp[childEmail] = result
                    }
                }
        } else {
            childrenDetailApp[childEmail]?.let {
                _listDetailApps.postValue(Pair(childEmail, it))
            }
        }
    }
    
    fun changeSortData(itemIndex: Int, childEmail: String) {
        childrenDetailApp[childEmail]?.let { list ->
            when (itemIndex) {
                0 -> {
                    val resultList = list.sortedBy { it.namaAplikasi }
                    _changeDetailApps.postValue(Pair(childEmail, resultList.toMutableList()))
                }
                1 -> {
                    _changeDetailApps.postValue(Pair(childEmail, list))
                }
                else -> {
                    val resultList = list.sortedByDescending { it.penggunaanInternet }
                    _changeDetailApps.postValue(Pair(childEmail, resultList.toMutableList()))
                }
            }
        }
    }
}