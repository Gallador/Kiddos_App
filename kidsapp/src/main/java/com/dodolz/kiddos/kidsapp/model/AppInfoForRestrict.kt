package com.dodolz.kiddos.kidsapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

// This model only for generate list of active app in Pengaturan - Pembatasan Layar, because active item in
// Pengaturan - Pembatasan Layar has a different layout item (look at that fancy dropdown for setting Pembatasan durasi)

@Parcelize
data class AppInfoForRestrict (
    var namaAplikasi: String? = null,
    var namaPaketAplikasi: String? = null,
    var durasiPembatasan: Int? = null
) : Parcelable