package com.dodolz.kiddos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dodolz.kiddos.model.detail.DetailApp
import com.dodolz.kiddos.model.detail.UninstalledApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppDetailViewmodel: ViewModel() {
    
    private val db: FirebaseFirestore = Firebase.firestore
    private var childrenDetailApp: MutableMap<String, MutableList<DetailApp>> = mutableMapOf()
    private var childrenUninstalledApp: MutableMap<String, MutableList<UninstalledApp>> = mutableMapOf()
    
    private val _listDetailApps: MutableLiveData<Pair<String, MutableList<DetailApp>>> by lazy {
        MutableLiveData<Pair<String, MutableList<DetailApp>>>()
    }
    private val _uninstalledApps: MutableLiveData<Pair<String, MutableList<UninstalledApp>>> by lazy {
        MutableLiveData<Pair<String, MutableList<UninstalledApp>>>()
    }
    private val _changeDetailApps: MutableLiveData<Pair<String, MutableList<DetailApp>>> by lazy {
        MutableLiveData<Pair<String, MutableList<DetailApp>>>()
    }
    
    val listDetailApps: LiveData<Pair<String, MutableList<DetailApp>>>
        get() = _listDetailApps
    val uninstalledApps: LiveData<Pair<String, MutableList<UninstalledApp>>>
        get() = _uninstalledApps
    val changeDetailApps: LiveData<Pair<String, MutableList<DetailApp>>>
        get() = _changeDetailApps
    
    fun loadDetailApps(childEmail: String, forceLoad: Boolean = false)
            = viewModelScope.launch(Dispatchers.IO) {

        if (childrenDetailApp[childEmail] == null || forceLoad) {
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

    fun getUnistalledApps(childEmail: String) = viewModelScope.launch(Dispatchers.IO) {
        if (childrenUninstalledApp[childEmail] == null) {
            val result: MutableList<UninstalledApp> = mutableListOf()
            db.collection("User").document(childEmail).collection("Aplikasi Dihapus")
                .orderBy("waktuHapus", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener { task ->
                    task.result?.let {
                        for (document in it) {
                            result.add(document.toObject())
                        }
                        _uninstalledApps.postValue(Pair(childEmail, result))
                        childrenUninstalledApp[childEmail] = result
                    }
                }
        } else {
            childrenUninstalledApp[childEmail]?.let {
                _uninstalledApps.postValue(Pair(childEmail, it))
            }
        }
    }

}