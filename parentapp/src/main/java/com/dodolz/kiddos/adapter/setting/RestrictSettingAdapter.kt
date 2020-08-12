package com.dodolz.kiddos.adapter.setting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dodolz.kiddos.R
import com.dodolz.kiddos.model.setting.AppInfoForRestrict
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.item_in_rv_setting_pembatasan.view.*

interface OnSwitchRestrictClickCallback {
    fun onSwitchClicked(data: AppInfoForRestrict, switch: Switch)
}

interface OnDropdownClickCallback {
    fun onDropdownCLicked(data: AppInfoForRestrict, itemInHour: Int)
}

// This adapter only for generate list of active app in Pengaturan - Pembatasan Layar, because active item in
// Pengaturan - Pembatasan Layar has a different layout item (look at that fancy dropdown for setting Pembatasan durasi)
class RestrictSettingAdapter(private val childEmail: String, private val activeState: Boolean)
    : RecyclerView.Adapter<RestrictSettingAdapter.MainListViewHolder>() {
    
    private lateinit var switchClickCallback: OnSwitchRestrictClickCallback
    private lateinit var dropdownClickCallback: OnDropdownClickCallback
    private lateinit var restrictDurationChoices: Array<String>
    private lateinit var restrictDurationChoicesAdapter: ArrayAdapter<String>
    private var appList: MutableList<AppInfoForRestrict> = mutableListOf()
    
    fun setOnSwitchClickCallback(switchClick: OnSwitchRestrictClickCallback) {
        this.switchClickCallback = switchClick
    }
    
    fun setOnDropdownCLickCallback(dropdownClick: OnDropdownClickCallback) {
        this.dropdownClickCallback = dropdownClick
    }
    
    fun clearData() {
        appList.clear()
        notifyDataSetChanged()
    }
    
    fun setData(data: MutableList<AppInfoForRestrict>) {
        appList.clear()
        appList.addAll(data)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_in_rv_setting_pembatasan, parent, false)
        restrictDurationChoices = arrayOf("1 Jam", "2 Jam", "3 Jam")
        restrictDurationChoicesAdapter = ArrayAdapter(parent.context,
            R.layout.setting_dropdown_item, restrictDurationChoices)
        return MainListViewHolder(
            view,
            childEmail,
            restrictDurationChoices,
            restrictDurationChoicesAdapter
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
        holder.itemView.txt_pilihanDurasiPembatasan.onItemClickListener = AdapterView.OnItemClickListener {
                _: AdapterView<*>, _: View, itemIndex: Int, _: Long ->
            dropdownClickCallback.onDropdownCLicked(appList[pos], itemIndex)
        }
    }
    
    class MainListViewHolder(itemView: View, childEmail: String,
                             private val array: Array<String>,
                             private val arrayAdapter: ArrayAdapter<String>)
        : RecyclerView.ViewHolder(itemView) {
        private val storage = Firebase.storage
        private val storageRef = storage.reference.child("$childEmail/iconApp")
        fun bindApp(data: AppInfoForRestrict, activeState: Boolean) {
            with(itemView) {
                storageRef.child("${data.namaPaketAplikasi}.png").downloadUrl
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
                txt_namaApp.text = data.namaAplikasi
                switch_aktivasi.isChecked = activeState
                txt_pilihanDurasiPembatasan.apply {
                    setAdapter(arrayAdapter)
                    data.durasiPembatasan?.let{
                        setText(array[it - 1], false)
                    }
                }
            }
        }
    }
}