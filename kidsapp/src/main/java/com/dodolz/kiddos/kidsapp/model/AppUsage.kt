package com.dodolz.kiddos.kidsapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AppUsage (
    var namaPaketAplikasi: String,
    var namaAplikasi: String,
    var penggunaanInternet: Long,
    var durasiPenggunaan: Long
) : Parcelable