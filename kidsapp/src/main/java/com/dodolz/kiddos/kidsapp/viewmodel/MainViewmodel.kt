package com.dodolz.kiddos.kidsapp.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dodolz.kiddos.kidsapp.model.AppIcon
import com.dodolz.kiddos.kidsapp.model.ChildSetting
import com.dodolz.kiddos.kidsapp.model.PhoneUsage
import com.dodolz.kiddos.kidsapp.repository.MainRepository
import com.dodolz.kiddos.kidsapp.util.PhoneUsageStatsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainViewmodel(application: Application): AndroidViewModel(application) {
    
    private val app = application
    private val mainRepository: MainRepository = MainRepository()
    private val _phoneUsageLiveData: MutableLiveData<Triple<String, PhoneUsage, Map<String, AppIcon>>> by lazy {
        MutableLiveData<Triple<String, PhoneUsage, Map<String, AppIcon>>>()
    }
    private val _childSetting: MutableLiveData<Pair<String, ChildSetting>> by lazy {
        MutableLiveData<Pair<String, ChildSetting>>()
    }
    
    val phoneUsage: LiveData<Triple<String, PhoneUsage, Map<String, AppIcon>>>
        get() = _phoneUsageLiveData
    val childSetting: LiveData<Pair<String, ChildSetting>>
        get() = _childSetting
    
    fun sendLocation(lat: Double, long: Double, email: String) = viewModelScope.launch(Dispatchers.IO) {
        mainRepository.sendLocation(lat, long, email)
    }
    
    fun getPhoneUsageStats(email: String, subscriberId: String){
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.READ_PHONE_STATE)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                val (phoneUsage, mapOfAppIcon) =
                     PhoneUsageStatsUtils.getUsageStatictics(app, subscriberId)
                _phoneUsageLiveData.postValue(Triple(email, phoneUsage, mapOfAppIcon))
                
                launch(Dispatchers.IO) {
                    mainRepository.sendPhoneUsageDataToDb(phoneUsage, email)
                }
                launch(Dispatchers.IO) {
                    mainRepository.sendAppIconToStorage(mapOfAppIcon, app.filesDir.absolutePath, email)
                }
            }
        }
    }
}
