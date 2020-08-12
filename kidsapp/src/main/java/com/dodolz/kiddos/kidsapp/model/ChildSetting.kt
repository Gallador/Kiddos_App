package com.dodolz.kiddos.kidsapp.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ChildSetting (
    var recordDuration: Int,
    var activeRecordList: MutableMap<String, AppInfo>,
    var activeRestrictList: MutableMap<String, AppInfoForRestrict>,
    var activeBlockList: MutableMap<String, AppInfo>
) : Parcelable
