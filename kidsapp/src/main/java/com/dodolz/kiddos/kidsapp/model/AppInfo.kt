package com.dodolz.kiddos.kidsapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AppInfo(
    var namaAplikasi: String? = null,
    var namaPaketAplikasi: String? = null
) : Parcelable
