package com.dodolz.kiddos.kidsapp.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AppIcon (
    val appIcon: Bitmap
) : Parcelable