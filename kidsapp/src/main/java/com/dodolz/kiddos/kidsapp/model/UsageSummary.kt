package com.dodolz.kiddos.kidsapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class UsageSummary (
    var appPalingLamaDiakses: ArrayList<String>,
    var totalDurasiPenggunaanSmartphone: Long,
    var totalPenggunaanInternet: Long,
    var timestampPemutakhiranData: Long
) : Parcelable