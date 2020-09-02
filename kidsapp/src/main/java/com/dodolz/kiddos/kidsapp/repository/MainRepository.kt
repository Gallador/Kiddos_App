package com.dodolz.kiddos.kidsapp.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.dodolz.kiddos.kidsapp.model.AppIcon
import com.dodolz.kiddos.kidsapp.model.PhoneUsage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainRepository {
    
    private val db = Firebase.firestore
    
    fun sendPhoneUsageDataToDb(phoneUsage: PhoneUsage, email: String) {
        val userRef = db.collection("User").document(email)
        val usageDetailCollectionRef = userRef.collection("Detail Penggunaan")
        val listOfAllAppCollectionRef = userRef.collection("Daftar Aplikasi")
        val appHistoryRef = userRef.collection("Riwayat Akses Aplikasi")
        
        val usageSum = phoneUsage.usageSummary
        val mapOfAppDetail = phoneUsage.mapOfAppDetail.toMutableMap()
        val mapOfAppHistory = phoneUsage.mapOfAppHistory.toMutableMap()
        
        // Mengisi nilai url dari icon app yang paling lama digunakan
        // Mengupdate nilai untuk rangkuman penggunaan smartphone
        userRef.update("appPalingLamaDiakses", usageSum.appPalingLamaDiakses)
        userRef.update("totalDurasiPenggunaanSmartphone", usageSum.totalDurasiPenggunaanSmartphone)
        userRef.update("totalPenggunaanInternet", usageSum.totalPenggunaanInternet)
        userRef.update("timestampPemutakhiranData", usageSum.timestampPemutakhiranData)
        
        // Delete dulu semua doc yang ada di koleksi Detail Penggunaan
        usageDetailCollectionRef.get().addOnSuccessListener {
            for (item in it.documents) {
                if (mapOfAppDetail[item["namaPaketAplikasi"]] == null)
                    usageDetailCollectionRef.document(item.id).delete()
            }
        }
        
        // Mengupload data penggunaan per masing-masing app
        for ((_, appUsage) in mapOfAppDetail) {
            usageDetailCollectionRef.document(appUsage.namaAplikasi).set(appUsage)
            listOfAllAppCollectionRef.document(appUsage.namaAplikasi).set(
                hashMapOf("namaAplikasi" to appUsage.namaAplikasi, "namaPaketAplikasi" to appUsage.namaPaketAplikasi)
            )
            try {
                userRef.collection("Aplikasi Dihapus").document(appUsage.namaAplikasi).delete()
            } catch (err: FirebaseFirestoreException) { }
        }
        
        //Mengupload data riwayat penggunaan aplikasi
        for ((_, appHistory) in mapOfAppHistory) {
            appHistoryRef.document(appHistory.waktuAkses.toString()).set(appHistory)
        }
    }
    
    fun sendLocation(lat: Double, long: Double, email: String) {
        Firebase.firestore.collection("User")
            .document(email).collection("Lokasi").document("lokasi")
            .set(hashMapOf(
                "lat" to lat,
                "long" to long,
                "waktuDimutakhirkan" to System.currentTimeMillis()
            ))
    }
    
    fun sendAppIconToStorage(sourceMap: Map<String, AppIcon>, storagePath: String, email: String) {
        val storageRef = Firebase.storage.reference
        val appIconRef = storageRef.child("$email/iconApp")
        val pathFile = "$storagePath/appIcon"
        if (!File(pathFile).exists()) {
            if (File(pathFile).mkdirs()) {
                Log.i("Folder ", "created")
            }
        }
        val listOfAppIcon: MutableMap<String, Boolean> = mutableMapOf()
        val a = appIconRef.listAll()
        Tasks.await(a).items.forEach {
            listOfAppIcon[it.name.dropLast(4)] = true
        }
        for ((packageName, appIcon) in sourceMap) {
            if (listOfAppIcon[packageName] == null) {
                val bitmap = appIcon.appIcon
                val appIconImagesRef = appIconRef.child("${packageName}.png")
                try {
                    val file = File(pathFile, "${packageName}.png")
                    val fileOut = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOut)
                    appIconImagesRef.putFile(Uri.fromFile(file)).addOnCompleteListener {
                        fileOut.flush()
                        fileOut.close()
                        File(file.absolutePath).delete()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
    
}