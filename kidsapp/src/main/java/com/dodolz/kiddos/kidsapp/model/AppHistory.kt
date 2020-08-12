package com.dodolz.kiddos.kidsapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AppHistory (
    var namaPaketAplikasi: String,
    var namaAplikasi: String,
    var waktuAkses: Long
) : Parcelable