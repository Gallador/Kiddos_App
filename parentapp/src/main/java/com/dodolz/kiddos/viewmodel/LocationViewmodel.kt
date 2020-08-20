package com.dodolz.kiddos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewmodel: ViewModel() {
    private val _isUserRefreshing: MutableLiveData<Pair<Boolean, String>> by lazy {
        MutableLiveData<Pair<Boolean, String>>()
    }
    val isUserRefreshing: LiveData<Pair<Boolean, String>>
        get() = _isUserRefreshing

    fun refreshData(childEmail: String) {
        _isUserRefreshing.postValue(Pair(true, childEmail))
    }
}