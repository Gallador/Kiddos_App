package com.dodolz.kiddos.kidsapp.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.dodolz.kiddos.kidsapp.model.AppInfo
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class GetUninstalledApps(private val context: Context) {
    fun getUninstalledApp(childEmail: String) {
        val dbRef = Firebase.firestore.collection("User").document(childEmail)
        dbRef.collection("Daftar Aplikasi").get()
            .addOnSuccessListener {
                for (doc in it) {
                    val appInfo: AppInfo = doc.toObject()
                    appInfo.namaPaketAplikasi?.let { packName ->
                        if (getAppInfo(packName) == null) deleteAppForDB(appInfo, childEmail)
                    }
                }
            }
    }

    private fun deleteAppForDB(appInfo: AppInfo, childEmail: String) {
        val dbRef = Firebase.firestore.collection("User").document(childEmail)
        appInfo.namaAplikasi?.let {
            dbRef.collection("Aplikasi Dihapus").document(it).set(
                hashMapOf("namaAplikasi" to it,
                    "namaPaketAplikasi" to appInfo.namaPaketAplikasi,
                    "waktuHapus" to System.currentTimeMillis())
            )
            dbRef.collection("Detail Penggunaan").document(it).delete()
            dbRef.collection("Daftar Aplikasi").document(it).delete()
            dbRef.collection("Pengaturan").document("Perekaman")
                .collection("Aktif").document(it).delete()
            dbRef.collection("Pengaturan").document("Pembatasan")
                .collection("Aktif").document(it).delete()
            dbRef.collection("Pengaturan").document("Blokir")
                .collection("Aktif").document(it).delete()
        }
    }

    private fun getAppInfo(packageName: String): ApplicationInfo? {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
}