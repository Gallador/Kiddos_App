package com.dodolz.kiddos.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import com.dodolz.kiddos.model.video.MainItem
import com.dodolz.kiddos.model.video.SubItem
import com.dodolz.kiddos.model.video.VideoInfo
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.set


@SuppressLint("SimpleDateFormat")
class VideoViewmodel(private val app: Application): AndroidViewModel(app) {
    
    private val storage = Firebase.storage
    private val db: FirebaseFirestore = Firebase.firestore
    private val childrenVideoList: MutableMap<String, MutableList<MainItem>> = mutableMapOf()
    
    private val _listOfVideo: MutableLiveData<Pair<String, MutableList<MainItem>>> by lazy {
        MutableLiveData<Pair<String, MutableList<MainItem>>>()
    }
    //---------------------------------------------<ChildEmail, VideoInfo>
    private val _requestStatus: MutableLiveData<Pair<String, VideoInfo>> by lazy {
        MutableLiveData<Pair<String, VideoInfo>>()
    }
    private val _downloadUrlReady: MutableLiveData<Pair<String, VideoInfo>> by lazy {
        MutableLiveData<Pair<String, VideoInfo>>()
    }
    private val _videoPath: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }
    
    val listOfVideo: LiveData<Pair<String, MutableList<MainItem>>>
        get() = _listOfVideo
    val requestStatus: LiveData<Pair<String, VideoInfo>>
        get() = _requestStatus
    val downloadUrlReady: LiveData<Pair<String, VideoInfo>>
        get() = _downloadUrlReady
    val videoPath: LiveData<String>
        get() = _videoPath
    
    fun getListOfVideos(childEmail: String)= viewModelScope.launch(Dispatchers.IO) {
        if (childrenVideoList[childEmail] == null) {
            val childVideoRef = storage.reference.child("$childEmail/video")
            val listOfItem: MutableList<MainItem> = mutableListOf()
            val getMainItem = childVideoRef.listAll()
            Tasks.await(getMainItem).prefixes.forEach {
                val mainItem = MainItem()
                mainItem.dateDdMmYyyy = it.name
                mainItem.dateNormal = processDate(it.name)
                listOfItem.add(mainItem)
            }
            for (item in listOfItem) {
                val getSubItem = childVideoRef.child(item.dateDdMmYyyy).listAll()
                Tasks.await(getSubItem).prefixes.forEach {
                    val subItem = SubItem()
                    subItem.prefixPath = it.path
                    subItem.prefixName = it.name
                    subItem.appName = it.name.substringBefore('_')
                    subItem.recordTime = it.name.takeLast(8).dropLast(3)
                    item.subList.add(subItem)
                }
            }
            childrenVideoList[childEmail] = listOfItem
            _listOfVideo.postValue(Pair(childEmail, listOfItem))
        } else {
            _listOfVideo.postValue(Pair(childEmail, childrenVideoList[childEmail]!!))
        }
    }
    
    fun postRequestForVideo(userID: String, data: SubItem, childEmail: String)
        = viewModelScope.launch(Dispatchers.IO) {
        val videoFilePath = "${app.filesDir.absoluteFile}/videos/$childEmail"
        val reqID = "${userID}_${data.prefixName.filterNot{ a -> a == ':'}}"
        Log.d("REQID", reqID+ "-" + data.prefixPath.drop(1))
        if (!File(videoFilePath).exists()) File(videoFilePath).mkdirs()
        val specificVideoPath = "$videoFilePath/$reqID"
        if (File("$specificVideoPath/$reqID.mp4").exists()) {
            _videoPath.postValue("$specificVideoPath/$reqID.mp4")
        } else {
            val videoRef = db.collection("Video Processing").document("processedVideo")
            videoRef.get().addOnCompleteListener {
                if (it.result?.get(reqID) != null) {
                    val downloadUrl = it.result?.get(reqID).toString()
                    _requestStatus.postValue(
                        Pair(childEmail, VideoInfo("downloading", reqID, downloadUrl))
                    )
                } else {
                    AndroidNetworking
                        .post("https://kiddos-app-c0029.web.app/parent/video/$reqID")
                        .setContentType("application/json")
                        .addBodyParameter("requestID", reqID)
                        .addBodyParameter("requestedURL", data.prefixPath.drop(1))
                        .setPriority(Priority.HIGH)
                        .build()
                        .getAsJSONObject(object : JSONObjectRequestListener {
                            override fun onResponse(response: JSONObject?) {
                                _requestStatus.postValue(Pair(childEmail, VideoInfo("waiting", reqID)))
                            }
                            override fun onError(anError: ANError?) {
                                _requestStatus.postValue(Pair(childEmail, VideoInfo("fail")))
                            }
                        })
                }
            }
        }
    }
    
    // Will invoked when reqStatus = waiting but the video not yet available
    fun watchProcessedVideo(childEmail: String, reqID: String) = viewModelScope.launch(Dispatchers.IO) {
        var downloadUrl = ""
        while (downloadUrl == "") {
            
            val task = db.collection("Video Processing").document("processedVideo").get()
            Tasks.await(task)?.let { result ->
                if (result.get(reqID) != null) {
                    downloadUrl = result.get(reqID).toString()
                    _downloadUrlReady.postValue(Pair(childEmail, VideoInfo("", reqID, downloadUrl)))
                }
            }
        }
    }
    
    fun downloadVideo(childEmail: String, reqID: String, downloadUrl: String) {
        val videoFilePath = "${app.filesDir.absoluteFile}/videos/$childEmail"
        val specificVideoPath = "$videoFilePath/$reqID"
        PRDownloader
            .download(downloadUrl, specificVideoPath, "$reqID.mp4")
            .build()
            .setOnStartOrResumeListener {}
            .setOnPauseListener {}
            .setOnCancelListener {}
            .setOnProgressListener {}
            .start(object: OnDownloadListener {
                override fun onDownloadComplete() {
                    _videoPath.postValue("$specificVideoPath/$reqID.mp4")
                }
                override fun onError(error: Error?) {
                    _videoPath.postValue("fail")
                }
            })
    }
    private fun processDate(rawDate: String): String {
        val patternForRaw = "dd-MM-yyyy"
        val simpleDateFormat1 = SimpleDateFormat(patternForRaw)
        val date: Date? = simpleDateFormat1.parse(rawDate)
    
        val pattern1 = "EEEE"
        val pattern2 = ", d MMMM"
        val formatter1 = SimpleDateFormat(pattern1)
        val formatter2 = SimpleDateFormat(pattern2)
        var hari = ""
        var tglBulan = ""
        date?.let {
            hari = formatter1.format(it)
            tglBulan = formatter2.format(it)
        }
        return when (hari) {
            "Sunday" -> "Minggu$tglBulan"
            "Monday" -> "Senin$tglBulan"
            "Tuesday" -> "Selasa$tglBulan"
            "Wednesday" -> "Rabu$tglBulan"
            "Thursday" -> "Kamis$tglBulan"
            "Friday" -> "Jumat$tglBulan"
            "Saturday" -> "Sabtu$tglBulan"
            else -> "Hari$tglBulan"
        }
    }
}