@file:Suppress("DEPRECATION")
package com.dodolz.kiddos.kidsapp.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.MEDIA_PROJECTION_DATA
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.MEDIA_PROJECTION_RESULT_CODE
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.NOTIFICATION_CHANNEL_ID
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.ONGOING_NOTIFICATION_ID
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.SUB_ID
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.USER_EMAIL
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.VIRTUAL_DISPLAY_NAME
import com.dodolz.kiddos.kidsapp.R
import com.dodolz.kiddos.kidsapp.activity.BlockActivity
import com.dodolz.kiddos.kidsapp.activity.LimitUsageActivity
import com.dodolz.kiddos.kidsapp.model.AppInfo
import com.dodolz.kiddos.kidsapp.model.AppInfoForRestrict
import com.dodolz.kiddos.kidsapp.repository.MainRepository
import com.dodolz.kiddos.kidsapp.util.PhoneUsageStatsUtils
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainForegroundService : Service() {
    
    private lateinit var mMediaRecorder: MediaRecorder
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private var isBusyRecording: Boolean = false
    private var userEmail: String? = null
    private var subscriberId: String? = null
    private var recordDuration: Int = 1
    private lateinit var mainRepository: MainRepository
    private lateinit var functions: FirebaseFunctions
    private var activeRecordMap: MutableMap<String, AppInfo> = mutableMapOf()
    private var activeRestrictMap: MutableMap<String, AppInfoForRestrict> = mutableMapOf()
    private var activeBlockMap: MutableMap<String, AppInfo> = mutableMapOf()
    
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Main Notification Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.setShowBadge(false)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                notificationChannel
            )
            val notification: Notification = Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Kiddos")
                .setContentText("Kiddos Is Running")
                .setSmallIcon(R.drawable.ic_logo_test_transparent)
                .setSubText("")
                .build()
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        } else {
            val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Kiddos")
                .setContentText("Kiddos Is Running")
                .setSmallIcon(R.drawable.ic_logo_test_transparent)
                .setSubText("")
                .build()
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }
    
    @SuppressLint("SimpleDateFormat")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        mainRepository = MainRepository()
        isBusyRecording = false
        userEmail = intent.getStringExtra(USER_EMAIL)
        subscriberId = intent.getStringExtra(SUB_ID)
        functions = FirebaseFunctions.getInstance()
        // Set snapshot listener for Setting
        userEmail?.let {
            val docRef =
                Firebase.firestore.collection("User").document(it).collection("Pengaturan")
            docRef.document("Perekaman").addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) loadActiveAppForRecord(docRef)
            }
            docRef.document("Pembatasan").addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) loadActiveAppForRestrict(docRef)
            }
            docRef.document("Blokir").addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) loadActiveAppForBlock(docRef)
            }
        }
        
        val usageStatsManager = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE)
            as UsageStatsManager
        val alwaysOn = true
        var timeToSendToDb = true
        val intentToBlockActivity = Intent(applicationContext, BlockActivity::class.java)
        intentToBlockActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intentToBlockActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intentToBlockActivity.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        val launchedApps : SortedMap<Long, UsageEvents.Event> = TreeMap()
        
        // never ending loop to check the last launched app
        CoroutineScope(Dispatchers.IO).launch {
            while (alwaysOn) {
                val timeNow = SimpleDateFormat("mm").format(Date(System.currentTimeMillis()))
                if (timeNow == "15" && timeToSendToDb) {
                    timeToSendToDb = false
                    launch(Dispatchers.IO) {
                        subscriberId?.let { subId ->
                            val (phoneUsage, mapOfAppIcon) =
                                PhoneUsageStatsUtils.getUsageStatictics(applicationContext, subId)
                            userEmail?.let {
                                launch(Dispatchers.IO) {
                                    mainRepository.sendPhoneUsageDataToDb(phoneUsage, it)
                                }
                                launch(Dispatchers.IO) {
                                    mainRepository.sendAppIconToStorage(
                                        mapOfAppIcon,
                                        applicationContext.filesDir.absolutePath,
                                        it
                                    )
                                }
                                CoroutineScope(Dispatchers.Main).launch {
                                    getLocation(it)
                                }
                            }
                        }
                    }
                } else if (timeNow != "15") timeToSendToDb = true
                delay(500L)
                val currentTime = System.currentTimeMillis()
                val usageEvents = usageStatsManager.queryEvents(currentTime - 500L, currentTime)
                while (usageEvents.hasNextEvent()) {
                    val currentEvent = UsageEvents.Event()
                    usageEvents.getNextEvent(currentEvent)
                    // 1 = event is a foreground event
                    if (currentEvent.eventType == 1/* && (
                            activeBlockMap[currentEvent.packageName] != null ||
                                activeRestrictMap[currentEvent.packageName] != null ||
                                activeRecordMap[currentEvent.packageName] != null
                            )*/
                    ) {
                        launchedApps[currentEvent.timeStamp] = currentEvent
                    }
                }
                if (!launchedApps.isEmpty()) {
                    val lastApp = launchedApps.entries.last().value.packageName
                    when {
                        activeBlockMap[lastApp] != null -> {
                            applicationContext.startActivity(intentToBlockActivity)
                        }
                        activeRestrictMap[lastApp] != null -> {
                            val durasiPembatasan = activeRestrictMap[lastApp]?.durasiPembatasan?.toLong()
                            val packageName = activeRestrictMap[lastApp]?.namaPaketAplikasi
                            if (durasiPembatasan != null && packageName != null) {
                                if (getUsageDuration(packageName) >= durasiPembatasan * 3600000L) {
                                    applicationContext.startActivity(Intent(applicationContext, LimitUsageActivity::class.java))
                                }
                            }
                        }
                        activeRecordMap[lastApp] != null -> {
                            if (!isBusyRecording) {
                                val appName = activeRecordMap[lastApp]?.namaAplikasi
                                appName?.also {
                                    startRecordScreen(intent, it, recordDuration, this)
                                }
                            }
                        }
                        else -> isBusyRecording = false
                    }
                    launchedApps.clear()
                }
            }
        }
        return START_STICKY
    }

    private fun getUsageDuration(packageName: String): Long {
        val todayDate = GregorianCalendar()
        todayDate.run {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            set(GregorianCalendar.HOUR_OF_DAY, 0)
            set(GregorianCalendar.MINUTE, 0)
            set(GregorianCalendar.SECOND, 1)
            set(GregorianCalendar.MILLISECOND, 0)
        }
        val currentTime = System.currentTimeMillis()
        val mUsageStatsManager =
            applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageEvents = mUsageStatsManager.queryEvents(todayDate.timeInMillis, currentTime)
        val allEvents: ArrayList<UsageEvents.Event> = arrayListOf()
        var durasiPenggunaan = 0L
        usageEvents.run {
            while (this.hasNextEvent()) {
                val event = UsageEvents.Event()
                this.getNextEvent(event)
                val isForegroundOrBackgroundEvent =
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                            event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
                if (isForegroundOrBackgroundEvent && event.packageName == packageName) {
                    allEvents.add(event)
                }
            }
        }
        for (i in 0 until (allEvents.size - 1)) {
            val currentEvent = allEvents[i]
            val nextEvent = allEvents[i + 1]
            if (currentEvent.eventType == 1 && nextEvent.eventType == 2 &&
                (currentEvent.className == nextEvent.className)
            ) {
                durasiPenggunaan += nextEvent.timeStamp - currentEvent.timeStamp
            }
        }
        return durasiPenggunaan
    }

    @SuppressLint("MissingPermission")
    fun getLocation(email: String) {
        val locationManager: LocationManager? = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsStatus = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        if (gpsStatus) {
            //Log.d("MAYBE", locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.latitude.toString())
            val locListener = object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    location?.also { it ->
                        mainRepository.sendLocation(it.latitude, it.longitude, email)
                        locationManager?.removeUpdates(this)
                    }
                }
                override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                override fun onProviderEnabled(p0: String?) {}
                override fun onProviderDisabled(p0: String?) {}
            }
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0,
                0f, locListener)
        }
    }
    
    // Functions for load new setting after listener invoked
    private fun loadActiveAppForRecord(docRef: CollectionReference) {
        val activeList: MutableMap<String, AppInfo> = mutableMapOf()
        val perekamanDocument = docRef.document("Perekaman")
        // To get record duration
        perekamanDocument.get().addOnSuccessListener {
            it.get("durasiPerekaman")?.let{ durasi -> recordDuration = durasi.toString().toInt() }
        }
        // To get apps with screen record activated
        perekamanDocument.collection("Aktif").get().addOnSuccessListener { query ->
            for (doc in query) {
                activeList[doc.get("namaPaketAplikasi").toString()] = doc.toObject()
            }
            activeRecordMap = activeList
        }
    }
    private fun loadActiveAppForRestrict(docRef: CollectionReference) {
        val activeList: MutableMap<String, AppInfoForRestrict> = mutableMapOf()
        val pembatasanDocument = docRef.document("Pembatasan")
        // To get apps with screen restriction activated
        pembatasanDocument.collection("Aktif").get()
            .addOnSuccessListener { query ->
                for (doc in query) {
                    activeList[doc.get("namaPaketAplikasi").toString()] = doc.toObject()
                }
                activeRestrictMap = activeList
            }
    }
    private fun loadActiveAppForBlock(docRef: CollectionReference) {
        val activeList: MutableMap<String, AppInfo> = mutableMapOf()
        val blokirDocument = docRef.document("Blokir")
        // To get apps with block activated
        blokirDocument.collection("Aktif").get()
            .addOnSuccessListener { query ->
                for (doc in query) {
                    activeList[doc.get("namaPaketAplikasi").toString()] = doc.toObject()
                }
                activeBlockMap = activeList
            }
    }
    
    @SuppressLint("SimpleDateFormat")
    private fun startRecordScreen(
        intent: Intent,
        appName: String,
        duration: Int,
        coroutineScope: CoroutineScope
    ) {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mData: Intent? = intent.getParcelableExtra(MEDIA_PROJECTION_DATA)
        val mResultCode = intent.getIntExtra(MEDIA_PROJECTION_RESULT_CODE, -1)
        val mPath = applicationContext.filesDir.absolutePath + "/video"
        if (!File(mPath).exists()) File(mPath).mkdirs()
        val recordTimeForFolder = SimpleDateFormat("dd-MM-yy_HH:mm:ss").format(Date(System.currentTimeMillis()))
        val recordTimeForFile = recordTimeForFolder.filterNot{ a -> a == ':'} // -> dd-MM-yy_HHmmss
        val folderFileName = "${appName.filterNot{ a -> a.isWhitespace()}}_${recordTimeForFolder}"
        val videoFileName = "${appName.filterNot{ a -> a.isWhitespace()}}_${recordTimeForFile}.mp4"
        //val videoFile = File(mPath, videoFileName)
        val videoFilePath = File(mPath, videoFileName).absolutePath
        val mScreenDensity = Resources.getSystem().displayMetrics.densityDpi
        mData?.let {
            mMediaProjection = manager.getMediaProjection(mResultCode, it) as MediaProjection
        }
        mMediaProjection?.also {
            mMediaRecorder = MediaRecorder()
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder.setOutputFormat(2) // For MPEG_4
            try {
                mMediaRecorder.setVideoEncoder(5) // For HEVC Encoder
            } catch (ex: IllegalArgumentException) {
                mMediaRecorder.setVideoEncoder(0) // back to default
            }
            
            mMediaRecorder.setOutputFile(videoFilePath)
            mMediaRecorder.setVideoSize(480, 854)
            mMediaRecorder.setVideoEncodingBitRate(2_500_000) // For bitrate 2.5 Mbps
            mMediaRecorder.setVideoFrameRate(24)
            mMediaRecorder.prepare()
            
            mVirtualDisplay = mMediaProjection?.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                480,
                854,
                mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.surface,
                null,
                null
            )
            
            mMediaRecorder.start()
            isBusyRecording = true
            coroutineScope.launch(Dispatchers.IO) {countRecorderTimer(duration, videoFilePath, folderFileName)}
        }
    }
    
    private suspend fun countRecorderTimer(
        duration: Int,
        videoFilePath: String,
        folderFileName: String
    ) {
        if (isBusyRecording) {
                delay((duration.toLong() * 5_000L) + 1_000L)
                // Stop screen recording
                mVirtualDisplay?.release()
                mVirtualDisplay = null
                mMediaRecorder.setOnErrorListener(null)
                mMediaProjection?.stop()
                mMediaRecorder.reset()
                mMediaRecorder.release()
                mMediaProjection = null
                var isFileReady = false
                while (!isFileReady) {
                    if (File(videoFilePath).exists()) {
                        sendVideoToStorage(videoFilePath, folderFileName)
                        isFileReady = true
                    }
                }
            }
    }
    
    @SuppressLint("SimpleDateFormat")
    private fun sendVideoToStorage(videoFilePath: String, folderFileName: String) {
        userEmail?.also {
            val videoFile = File(videoFilePath)
            val folderName = SimpleDateFormat("dd-MM-yyyy").format(Date(System.currentTimeMillis()))
            val folderPath = "$it/video/$folderName/$folderFileName/encrypted"
            val storageRef = Firebase.storage.reference
                .child(folderPath)
            storageRef.child(videoFile.name).putFile(Uri.fromFile(videoFile)).addOnSuccessListener {
                //triggerEn("$folderPath/${videoFile.name}")
                File(videoFile.absolutePath).delete()
            }
        }
    }
    
    /*private fun triggerEn(text: String): Task<String> {
        Log.d("INVOKED", "Func")
        // Create the arguments to the callable function.
        val data = hashMapOf(
            "name" to text
        )
        return functions
            .getHttpsCallable("encryptFile")
            .call(data)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data as String
                result
            }
    }*/
    
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}