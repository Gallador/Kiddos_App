@file:Suppress("DEPRECATION")

package com.dodolz.kiddos.kidsapp.util

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.RemoteException
import com.dodolz.kiddos.kidsapp.model.*
import java.util.*


object PhoneUsageStatsUtils {
    
    fun getUsageStatictics(context: Context, subscriberId: String)
        : Pair<PhoneUsage, Map<String, AppIcon>> {
        
        val todayDate = GregorianCalendar()
        todayDate.run {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
            set(GregorianCalendar.HOUR_OF_DAY, 0)
            set(GregorianCalendar.MINUTE, 0)
            set(GregorianCalendar.SECOND, 1)
            set(GregorianCalendar.MILLISECOND, 0)
        }
        val currentTime = System.currentTimeMillis()
        var totalUsageDuration = 0L
        var totalInternetUsage = 0L
        val tempListAppHistory: SortedMap<Long, AppHistory> = TreeMap(Collections.reverseOrder())
        val finalListAppHistory: SortedMap<Long, AppHistory> = TreeMap(Collections.reverseOrder())
        val networkStatsManager =
            context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val (
            allEvents,
            listAppDetail,
            listAppIcon
        ) = getUsageEvents(context, todayDate.timeInMillis, currentTime)
    
        // Untuk menghitung waktu penggunaan per masing-masing app
        // dengan melakukan penjumlahan total waktu selisih foreground dengan background
        for (i in 0 until (allEvents.size - 1) step 2) {
            val currentEvent = allEvents[i]
            val nextEvent = allEvents[i + 1]
            if (currentEvent.eventType == 1 && nextEvent.eventType == 2 &&
                (currentEvent.className == nextEvent.className)
            ) {
                val timeDiff = nextEvent.timeStamp - currentEvent.timeStamp
                val appName = listAppDetail[currentEvent.packageName]?.namaAplikasi
                listAppDetail[currentEvent.packageName]?.let {
                    it.durasiPenggunaan += timeDiff
                    tempListAppHistory[currentEvent.timeStamp] =
                        AppHistory(currentEvent.packageName, (appName ?: ""), currentEvent.timeStamp)
                }
            }
        }
        
        // Mendapatkan riwayat pemakaian internet per masing-masing app
        fun getNetworkUsage (networkStats: NetworkStats?, uid: Int): Long {
            var internetUsage = 0L
            networkStats?.let {
                do {
                    val networkBucket = NetworkStats.Bucket()
                    it.getNextBucket(networkBucket)
                    if (networkBucket.uid == uid) internetUsage = (networkBucket.rxBytes + networkBucket.txBytes)
                } while (it.hasNextBucket())
            }
            return internetUsage
        }
        if (!listAppDetail.isNullOrEmpty()) {
            for ((key, appUsage) in listAppDetail) {
                // INI ANEH. Request untuk network stats harus dilakukan di setiap iterasi untuk setiap item
                // dalam listAppDetail. Bila request ini dilakukan hanya sekali dan diletak di luar loop
                // maka nilai yang didapatkan tidak akurat
                var networkWifiStats: NetworkStats? = null
                var networkCellularStats: NetworkStats? = null
                try {
                    networkWifiStats = networkStatsManager.querySummary(
                        1,
                        subscriberId,
                        todayDate.timeInMillis,
                        currentTime
                    )
                    networkCellularStats = networkStatsManager.querySummary(
                        0,
                        subscriberId,
                        todayDate.timeInMillis,
                        currentTime
                    )
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
                val (appInfo, _) = getAppInfoAndName(context, appUsage.namaPaketAplikasi)
                appInfo?.run {
                    listAppDetail[key]?.let {
                        it.penggunaanInternet =
                            getNetworkUsage(networkWifiStats, this.uid) +
                            getNetworkUsage(networkCellularStats, this.uid)
                        totalUsageDuration += it.durasiPenggunaan
                        totalInternetUsage += it.penggunaanInternet
                    }
                }
                networkCellularStats?.close()
                networkWifiStats?.close()
            }
        }
        
        // Untuk bagian Riwayat Akses Aplikasi
        // Mem-filter item pada tempListAppHistory untuk dimasukkan ke finalListAppHistory
        if (tempListAppHistory.isNotEmpty()) {
            tempListAppHistory.firstKey()?.let { firstKey ->
                val tempAppHistoryObj = object {
                    var appLaunchTime: Long? = firstKey
                    var appName: String? = tempListAppHistory[firstKey]?.namaAplikasi
                }
                finalListAppHistory[firstKey] = tempListAppHistory[firstKey]
                for ((key, appHistory) in tempListAppHistory.minus(firstKey)) {
                    tempAppHistoryObj.appName?.let { appName ->
                        if (key != tempAppHistoryObj.appLaunchTime && appHistory.namaAplikasi != appName) {
                            finalListAppHistory[key] = appHistory
                            tempAppHistoryObj.appLaunchTime = key
                            tempAppHistoryObj.appName = appHistory.namaAplikasi
                        }
                    }
                    if (finalListAppHistory.size == 15) {
                        break
                    }
                }
            }
        }
        var nameOfLongestUsedApp = "Belum Tersedia"
        var packageOfLongestUsedApp = ""
        val sortedListAppDetail = listAppDetail
            .filter { (_, value) -> value.durasiPenggunaan > 60000L }
            .toSortedMap(compareByDescending { listAppDetail[it]?.durasiPenggunaan })
        if (sortedListAppDetail.isNotEmpty()) {
            sortedListAppDetail[sortedListAppDetail.firstKey()]?.let {
                packageOfLongestUsedApp = it.namaPaketAplikasi
                nameOfLongestUsedApp = it.namaAplikasi
            }
        }
        val phoneUsage = PhoneUsage(
            UsageSummary(
                arrayListOf(nameOfLongestUsedApp, packageOfLongestUsedApp),
                totalUsageDuration,
                totalInternetUsage,
                System.currentTimeMillis()),
            finalListAppHistory,
            sortedListAppDetail
        )
        return Pair(phoneUsage, listAppIcon.toMap())
    }
    
    private fun getUsageEvents(
        context: Context,
        today00: Long,
        currentTime: Long
    ): Triple<ArrayList<UsageEvents.Event>, MutableMap<String, AppUsage>, MutableMap<String, AppIcon>> {
        val allEvents: ArrayList<UsageEvents.Event> = arrayListOf()
        val listAppDetail: MutableMap<String, AppUsage> = mutableMapOf()
        val listAppIcon: MutableMap<String, AppIcon> = mutableMapOf()
        val mUsageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val usageEvents = mUsageStatsManager.queryEvents(today00, currentTime)
    
        usageEvents.run {
            while (this.hasNextEvent()) {
                val event = UsageEvents.Event()
                this.getNextEvent(event)
                val isForegroundOrBackgroundEvent =
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
                val (appInfo, appName) = getAppInfoAndName(context, event.packageName)
                
                if (isForegroundOrBackgroundEvent && appInfo != null && appName != null &&
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && !isAppIncluded(appName)
                ) {
                    allEvents.add(event)
                    // taking it into a collection to access by package name
                    if (listAppDetail[event.packageName] == null) {
                        listAppDetail[event.packageName] =
                            AppUsage(event.packageName, appName, 0L, 0L)
                    }
                    if (listAppIcon[event.packageName] == null) {
                        val appIcon = context.packageManager.getApplicationIcon(event.packageName)
                        val icon = getBitmapFromDrawable(appIcon)
                        icon.let {
                            listAppIcon[event.packageName] = AppIcon(it)
                        }
                    }
                }
            }
        }
        return Triple(allEvents, listAppDetail, listAppIcon)
    }
    
    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }
    
    private fun getAppInfoAndName(
        context: Context,
        packageName: String
    ): Pair<ApplicationInfo?, String?> {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val appName = context.packageManager.getApplicationLabel(appInfo).toString()
            Pair(appInfo, appName)
        } catch (e: PackageManager.NameNotFoundException) {
            Pair(null, null)
        }
    }
    
    private fun isAppIncluded(appName: String): Boolean {
        return appName == "Kiddos Kids" || appName == "Kiddos Parent" ||appName.contains("Launcher") ||
        appName.contains("launcher") || appName.contains("Home") ||
        appName.contains("home")
    }
}