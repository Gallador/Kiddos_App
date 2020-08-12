package com.dodolz.kiddos.kidsapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class PhoneUsage (
    val usageSummary: UsageSummary,
    val mapOfAppHistory: SortedMap<Long, AppHistory>,
    val mapOfAppDetail: SortedMap<String, AppUsage>
) : Parcelable