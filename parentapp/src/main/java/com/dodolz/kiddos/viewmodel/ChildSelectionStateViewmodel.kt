package com.dodolz.kiddos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChildSelectionStateViewmodel: ViewModel() {
    // This viewmodel will be subscribed by all fragment in bottom nav view
    
    private val _childSelected: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    
    val childSelected: LiveData<String>
        get() = _childSelected
    
    fun changeChildSelected(childEmail: String) {
        _childSelected.postValue(childEmail)
    }
}