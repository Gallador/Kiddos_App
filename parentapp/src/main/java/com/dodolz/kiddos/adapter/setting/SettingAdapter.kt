package com.dodolz.kiddos.adapter.setting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dodolz.kiddos.R
import com.dodolz.kiddos.model.setting.AppInfo
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.item_in_rv_setting_just_switch.view.*

interface OnSwitchClickCallback {
    fun onSwitchClicked(data: AppInfo, switch: Switch)
}

class RecordSettingAdapter(private val childEmail: String, private val activeState: Boolean)
    : RecyclerView.Adapter<RecordSettingAdapter.MainListViewHolder>() {
    
    private lateinit var switchClickCallback: OnSwitchClickCallback
    private var appList: MutableList<AppInfo> = mutableListOf()
    
    fun setOnSwitchClickCallback(switchClick: OnSwitchClickCallback) {
        this.switchClickCallback = switchClick
    }
    
    fun clearData() {
        appList.clear()
        notifyDataSetChanged()
    }
    
    fun setData(data: MutableList<AppInfo>) {
        appList.clear()
        appList.addAll(data)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_in_rv_setting_just_switch, parent, false)
        return MainListViewHolder(
            view,
            childEmail
        )
    }
    
    override fun getItemCount(): Int {
        return appList.size
    }
    
    override fun onBindViewHolder(holder: MainListViewHolder, pos: Int) {
        holder.bindApp(appList[pos], activeState)
        holder.itemView.switch_aktivasi.setOnClickListener{
            switchClickCallback.onSwitchClicked(appList[pos], holder.itemView.switch_aktivasi)
        }
    }
    
    class MainListViewHolder(itemView: View, childEmail: String) : RecyclerView.ViewHolder(itemView) {
        private val storage = Firebase.storage
        private val storageRef = storage.reference.child("$childEmail/iconApp")
        fun bindApp(app: AppInfo, activeState: Boolean) {
            with(itemView) {
                storageRef.child("${app.namaPaketAplikasi}.png").downloadUrl
                    .addOnSuccessListener {
                        Glide.with(context)
                            .load(it)
                            .into(img_appIcon)
                    }
                    .addOnFailureListener {
                        Glide.with(context)
                            .load(R.drawable.ic_home_24)
                            .into(img_appIcon)
                    }
                txt_namaApp.text = app.namaAplikasi
                switch_aktivasi.isChecked = activeState
            }
        }
    }
}