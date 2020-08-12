package com.dodolz.kiddos.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dodolz.kiddos.model.setting.AppInfo
import com.dodolz.kiddos.model.setting.AppInfoForRestrict
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SettingViewmodel: ViewModel() {
    
    private val db: FirebaseFirestore = Firebase.firestore
    // ---------------------MutableMap<childEmail, MutableMap<appPackageName, -||- >
    private val childrenAppList: MutableMap<String, MutableMap<String, AppInfo>> = mutableMapOf()
    //Variable for Perekaman Layar untuk semua anak
    private val childrenActiveRecordList: MutableMap<String, MutableMap<String, AppInfo>> = mutableMapOf()
    private val childrenNonActiveRecordList: MutableMap<String, MutableMap<String, AppInfo>> = mutableMapOf()
    //Variable for Pembatasan Layar untuk semua anak
    private val childrenActiveRestrictList: MutableMap<String, MutableMap<String, AppInfoForRestrict>> = mutableMapOf()
    private val childrenNonActiveRestrictList: MutableMap<String, MutableMap<String, AppInfo>> = mutableMapOf()
    //Variable for Blokir Aplikasi untuk semua anak
    private val childrenActiveBlockList: MutableMap<String, MutableMap<String, AppInfo>> = mutableMapOf()
    private val childrenNonActiveBlockList: MutableMap<String, MutableMap<String, AppInfo>> = mutableMapOf()
    
    //LiveData for Perekaman Layar
    private val _activeRecordList: MutableLiveData<Pair<String, MutableList<AppInfo>>> by lazy {
        MutableLiveData<Pair<String, MutableList<AppInfo>>>()
    }
    private val _nonActiveRecordList: MutableLiveData<Pair<String, MutableList<AppInfo>>> by lazy {
        MutableLiveData<Pair<String, MutableList<AppInfo>>>()
    }
    private val _recordDuration: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }
    private val _activateRecordStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _deactivateRecordStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _setRecordDurationStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    
    //LiveData for Pembatasan Layar
    private val _activeRestrictList: MutableLiveData<Pair<String, MutableList<AppInfoForRestrict>>> by lazy {
        MutableLiveData<Pair<String, MutableList<AppInfoForRestrict>>>()
    }
    private val _nonActiveRestrictList: MutableLiveData<Pair<String, MutableList<AppInfo>>> by lazy {
        MutableLiveData<Pair<String, MutableList<AppInfo>>>()
    }
    private val _activateRestrictStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _deactivateRestrictStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _setRestrictDurationStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    
    //LiveData for Blokir Aplikasi
    private val _activeBlockList: MutableLiveData<Pair<String, MutableList<AppInfo>>> by lazy {
        MutableLiveData<Pair<String, MutableList<AppInfo>>>()
    }
    private val _nonActiveBlockList: MutableLiveData<Pair<String, MutableList<AppInfo>>> by lazy {
        MutableLiveData<Pair<String, MutableList<AppInfo>>>()
    }
    private val _activateBlockStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    private val _deactivateBlockStatus: MutableLiveData<Boolean> by lazy {
        MutableLiveData<Boolean>()
    }
    //
    /*private val _firebaseException: MutableLiveData<Exception> by lazy {
        MutableLiveData<Exception>()
    }*/
    
    //Public LiveData for Perekaman Layar
    val activeRecordList: LiveData<Pair<String, MutableList<AppInfo>>>
        get() = _activeRecordList
    val nonActiveRecordList: LiveData<Pair<String, MutableList<AppInfo>>>
        get() = _nonActiveRecordList
    val recordDuration: LiveData<Int>
        get() = _recordDuration
    val activationRecordStatus: LiveData<Boolean>
        get() = _activateRecordStatus
    val deactivationRecordStatus: LiveData<Boolean>
        get() = _deactivateRecordStatus
    val setRecordDurationStatus: LiveData<Boolean>
        get() = _setRecordDurationStatus
    
    //Public LiveData for Pembatasan Layar
    val activeRestrictList: LiveData<Pair<String, MutableList<AppInfoForRestrict>>>
        get() = _activeRestrictList
    val nonActiveRestrictList: LiveData<Pair<String, MutableList<AppInfo>>>
        get() = _nonActiveRestrictList
    val activationRestrictStatus: LiveData<Boolean>
        get() = _activateRestrictStatus
    val deactivationRestrictStatus: LiveData<Boolean>
        get() = _deactivateRestrictStatus
    val setRestrictDurationStatus: LiveData<Boolean>
        get() = _setRestrictDurationStatus
    
    //Public LiveData for Blokir Aplikasi
    val activeBlockList: LiveData<Pair<String, MutableList<AppInfo>>>
        get() = _activeBlockList
    val nonActiveBlockList: LiveData<Pair<String, MutableList<AppInfo>>>
        get() = _nonActiveBlockList
    val activationBlockStatus: LiveData<Boolean>
        get() = _activateBlockStatus
    val deactivationBlockStatus: LiveData<Boolean>
        get() = _deactivateBlockStatus
    
    //Functions for Perekaman Layar
    fun loadAppListForRecord(childEmail: String, force: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        val childDocument = db.collection("User").document(childEmail)
        fun loadNonActiveAppForRecord() {
            var nonActiveMap: Map<String, AppInfo>
            childrenAppList[childEmail]?.let { appList ->
                childrenActiveRecordList[childEmail]?.let { activeList ->
                     nonActiveMap = appList
                        .filterNot {
                            it.value.namaPaketAplikasi == activeList[it.value.namaPaketAplikasi]?.namaPaketAplikasi
                        }
                     childrenNonActiveRecordList[childEmail] = nonActiveMap.toMutableMap()
                     nonActiveMap.toMap().values.let {
                         _nonActiveRecordList.postValue(Pair(childEmail, it.toMutableList()))
                     }
                }
            }
        }
        fun loadActiveAppForRecord() {
            val activeList: MutableMap<String, AppInfo> = mutableMapOf()
            val perekamanDocument = childDocument.collection("Pengaturan").document("Perekaman")
            // To get record duration
            perekamanDocument.get()
                .addOnSuccessListener {
                    it.get("durasiPerekaman")?.let { durasi ->
                        if (durasi.toString().toInt() > 0)
                            _recordDuration.postValue(durasi.toString().toInt())
                        else
                            _recordDuration.postValue(1)
                    }
                }
                .addOnFailureListener { }
            // To get apps with screen record activated
            perekamanDocument.collection("Aktif").get()
                .addOnSuccessListener { query ->
                    for (doc in query) {
                        activeList[doc.get("namaPaketAplikasi").toString()] = doc.toObject()
                    }
                    childrenActiveRecordList[childEmail] = activeList
                    activeList.toMap().values.let {
                        _activeRecordList.postValue(Pair(childEmail, it.toMutableList()))
                    }
                    loadNonActiveAppForRecord()
                }
                .addOnFailureListener { }
        }
        // Force terpenuhi ketika terjadi penambahan / pengurangan aplikasi yang diaktifkan screen recording
        when {
            force -> { loadActiveAppForRecord() }
            childrenActiveRecordList[childEmail] == null && childrenNonActiveRecordList[childEmail] == null -> {
                val appListMap: MutableMap<String, AppInfo> = mutableMapOf()
                childDocument.collection("Daftar Aplikasi").get()
                    .addOnSuccessListener {
                        for (doc in it) {
                            appListMap[doc.get("namaPaketAplikasi").toString()] = doc.toObject()
                        }
                        childrenAppList[childEmail] = appListMap
                        loadActiveAppForRecord()
                    }
            }
            else -> {
                val active = childrenActiveRecordList[childEmail]?.toMap()
                val nonActive = childrenNonActiveRecordList[childEmail]?.toMap()
                active?.values?.let { _activeRecordList.postValue(Pair(childEmail, it.toMutableList())) }
                nonActive?.values?.let { _nonActiveRecordList.postValue(Pair(childEmail, it.toMutableList())) }
            }
        }
    }
    
    fun activateAppRecord(data: AppInfo, childEmail: String) {
        val childDocument = Firebase.firestore.collection("User").document(childEmail)
        data.namaAplikasi?.let {
            childDocument.collection("Pengaturan").document("Perekaman")
                .collection("Aktif").document(it).set(data)
                .addOnSuccessListener {
                    _activateRecordStatus.postValue(true)
                    childDocument.collection("Pengaturan").document("Perekaman")
                        .set(hashMapOf("durasiPerekaman" to (_recordDuration.value?.toInt() ?: 1), "waktuDimutakhirkan" to System.currentTimeMillis()))
                }
                .addOnFailureListener { _activateRecordStatus.postValue(false) }
        }
    }
    
    fun deactivateAppRecord(namaAplikasi: String, childEmail: String) {
        val childDocument = Firebase.firestore.collection("User").document(childEmail)
        childDocument.collection("Pengaturan").document("Perekaman")
            .collection("Aktif").document(namaAplikasi).delete()
            .addOnSuccessListener {
                _deactivateRecordStatus.postValue(true)
                childDocument.collection("Pengaturan").document("Perekaman")
                    .set(hashMapOf("durasiPerekaman" to (_recordDuration.value?.toInt() ?: 1), "waktuDimutakhirkan" to System.currentTimeMillis()))
            }
            .addOnFailureListener { _deactivateRecordStatus.postValue(false)}
    }
    
    fun setRecordDuration(minute: Int, childEmail: String) {
        val childDocument = Firebase.firestore.collection("User").document(childEmail)
        childDocument.collection("Pengaturan").document("Perekaman")
            .set(hashMapOf("durasiPerekaman" to minute, "waktuDimutakhirkan" to System.currentTimeMillis()))
            .addOnSuccessListener {
                _setRecordDurationStatus.postValue(true)
                _recordDuration.postValue(minute)
            }
            .addOnFailureListener { _setRecordDurationStatus.postValue(false) }
    }
    
    //Functions for Pembatasan Layar
    fun loadAppListForRestrict(childEmail: String, force: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        val childDocument = db.collection("User").document(childEmail)
        fun loadNonActiveAppForRestrict() {
            var nonActiveMap: Map<String, AppInfo>
            childrenAppList[childEmail]?.let { appList ->
                childrenActiveRestrictList[childEmail]?.let { activeList ->
                    nonActiveMap = appList
                        .filterNot {
                            it.value.namaPaketAplikasi == activeList[it.value.namaPaketAplikasi]?.namaPaketAplikasi
                        }
                    childrenNonActiveRestrictList[childEmail] = nonActiveMap.toMutableMap()
                    nonActiveMap.toMap().values.let {
                        _nonActiveRestrictList.postValue(Pair(childEmail, it.toMutableList()))
                    }
                }
            }
        }
        fun loadActiveAppForRestrict() {
            val activeList: MutableMap<String, AppInfoForRestrict> = mutableMapOf()
            val pembatasanDocument = childDocument.collection("Pengaturan").document("Pembatasan")
            // To get apps with screen restriction activated
            pembatasanDocument.collection("Aktif").get()
                .addOnSuccessListener { query ->
                    for (doc in query) {
                        activeList[doc.get("namaPaketAplikasi").toString()] = doc.toObject()
                    }
                    childrenActiveRestrictList[childEmail] = activeList
                    activeList.toMap().values.let {
                        _activeRestrictList.postValue(Pair(childEmail, it.toMutableList()))
                    }
                    loadNonActiveAppForRestrict()
                }
                .addOnFailureListener { }
        }
        if ((childrenActiveRestrictList[childEmail] == null && childrenNonActiveRestrictList[childEmail] == null) || force) {
            loadActiveAppForRestrict()
        } else {
            val active = childrenActiveRestrictList[childEmail]?.toMap()
            val nonActive = childrenNonActiveRestrictList[childEmail]?.toMap()
            active?.values?.let { _activeRestrictList.postValue(Pair(childEmail, it.toMutableList())) }
            nonActive?.values?.let { _nonActiveRestrictList.postValue(Pair(childEmail, it.toMutableList())) }
        }
    }
    
    fun activateAppRestrict(data: AppInfo, childEmail: String) {
        val childDocument = Firebase.firestore.collection("User").document(childEmail)
        data.namaAplikasi?.let {
            val newData = AppInfoForRestrict()
            newData.namaAplikasi = data.namaAplikasi
            newData.namaPaketAplikasi = data.namaPaketAplikasi
            newData.durasiPembatasan = 1
            childDocument.collection("Pengaturan").document("Pembatasan")
                .collection("Aktif").document(it).set(newData)
                .addOnSuccessListener {
                    _activateRestrictStatus.postValue(true)
                    childDocument.collection("Pengaturan").document("Pembatasan")
                        .set(hashMapOf("waktuDimutakhirkan" to System.currentTimeMillis()))
                }
                .addOnFailureListener { _activateRestrictStatus.postValue(false) }
        }
    }
    
    fun deactivateAppRestrict(namaAplikasi: String, childEmail: String) {
        val childDocument = Firebase.firestore.collection("User").document(childEmail)
        childDocument.collection("Pengaturan").document("Pembatasan")
            .collection("Aktif").document(namaAplikasi).delete()
            .addOnSuccessListener {
                _deactivateRestrictStatus.postValue(true)
                childDocument.collection("Pengaturan").document("Pembatasan")
                    .set(hashMapOf("waktuDimutakhirkan" to System.currentTimeMillis()))
            }
            .addOnFailureListener { _deactivateRestrictStatus.postValue(false)}
    }
    
    fun setRestrictDuration(data: AppInfoForRestrict, hour: Int, childEmail: String) {
        val childDocument = Firebase.firestore.collection("User").document(childEmail)
        data.durasiPembatasan = hour
        data.namaAplikasi?.let {
            childDocument.collection("Pengaturan").document("Pembatasan")
                .collection("Aktif").document(it).set(data)
                .addOnSuccessListener {
                    _setRestrictDurationStatus.postValue(true)
                    childDocument.collection("Pengaturan").document("Pembatasan")
                        .set(hashMapOf("waktuDimutakhirkan" to System.currentTimeMillis()))
                }
                .addOnFailureListener { _setRestrictDurationStatus.postValue(false)}
        }
    }
    
    //Functions for Blokir Aplikasi
    fun loadAppListForBlock(childEmail: String, force: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        val childDocument = db.collection("User").document(childEmail)
        fun loadNonActiveAppForBlock() {
            var nonActiveMap: Map<String, AppInfo>
            childrenAppList[childEmail]?.let { appList ->
                childrenActiveBlockList[childEmail]?.let { activeList ->
                    nonActiveMap = appList
                        .filterNot {
                            it.value.namaPaketAplikasi == activeList[it.value.namaPaketAplikasi]?.namaPaketAplikasi
                        }
                    childrenNonActiveBlockList[childEmail] = nonActiveMap.toMutableMap()
                    nonActiveMap.toMap().values.let {
                        _nonActiveBlockList.postValue(Pair(childEmail, it.toMutableList()))
                    }
                }
            }
        }
        fun loadActiveAppForBlock() {
            val activeList: MutableMap<String, AppInfo> = mutableMapOf()
            val blokirDocument = childDocument.collection("Pengaturan").document("Blokir")
            // To get apps with block activated
            blokirDocument.collection("Aktif").get()
                .addOnSuccessListener { query ->
                    for (doc in query) {
                        activeList[doc.get("namaPaketAplikasi").toString()] = doc.toObject()
                    }
                    childrenActiveBlockList[childEmail] = activeList
                    activeList.toMap().values.let {
                        _activeBlockList.postValue(Pair(childEmail, it.toMutableList()))
                    }
                    loadNonActiveAppForBlock()
                }
                .addOnFailureListener { }
        }
        // Force terpenuhi ketika terjadi penambahan / pengurangan aplikasi yang diaktifkan blokir
        if ((childrenActiveBlockList[childEmail] == null && childrenNonActiveBlockList[childEmail] == null) || force) {
            loadActiveAppForBlock()
        } else {
            val active = childrenActiveBlockList[childEmail]?.toMap()
            val nonActive = childrenNonActiveBlockList[childEmail]?.toMap()
            active?.values?.let { _activeBlockList.postValue(Pair(childEmail, it.toMutableList())) }
            nonActive?.values?.let { _nonActiveBlockList.postValue(Pair(childEmail, it.toMutableList())) }
        }
    }
    
    fun activateAppBlock(data: AppInfo, childEmail: String) {
        val childDocument = Firebase.firestore.collection("User").document(childEmail)
        data.namaAplikasi?.let {
            childDocument.collection("Pengaturan").document("Blokir")
                .collection("Aktif").document(it).set(data)
                .addOnSuccessListener {
                    _activateBlockStatus.postValue(true)
                    childDocument.collection("Pengaturan").document("Blokir")
                        .set(hashMapOf("waktuDimutakhirkan" to System.currentTimeMillis()))
                }
                .addOnFailureListener { _activateBlockStatus.postValue(false)}
        }
    }
    
    fun deactivateAppBlock(namaAplikasi: String, childEmail: String) {
        val childDocument = Firebase.firestore.collection("User").document(childEmail)
        childDocument.collection("Pengaturan").document("Blokir")
            .collection("Aktif").document(namaAplikasi).delete()
            .addOnSuccessListener {
                _deactivateBlockStatus.postValue(true)
                childDocument.collection("Pengaturan").document("Blokir")
                    .set(hashMapOf("waktuDimutakhirkan" to System.currentTimeMillis()))
            }
            .addOnFailureListener { _deactivateBlockStatus.postValue(false) }
    }
}