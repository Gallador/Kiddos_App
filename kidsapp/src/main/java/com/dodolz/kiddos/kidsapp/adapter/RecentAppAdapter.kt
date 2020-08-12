package com.dodolz.kiddos.kidsapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dodolz.kiddos.kidsapp.R
import com.dodolz.kiddos.kidsapp.model.AppHistory
import com.dodolz.kiddos.kidsapp.model.AppIcon
import kotlinx.android.synthetic.main.item_in_rv_recent_app.view.*
import java.text.SimpleDateFormat
import java.util.*

class RecentAppAdapter(
    private var recentAppList: List<AppHistory>,
    private val appIcon: Map<String, AppIcon>
): RecyclerView.Adapter<RecentAppAdapter.MainListViewHolder>() {
    
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MainListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_in_rv_recent_app, parent, false)
        return MainListViewHolder(
            view
        )
    }
    
    override fun getItemCount(): Int {
        return recentAppList.size
    }
    
    override fun onBindViewHolder(holder: MainListViewHolder, position: Int) {
        holder.bindItem(recentAppList[position], appIcon[recentAppList[position].namaPaketAplikasi])
    }
    
    class MainListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /*private val storage = Firebase.storage
        private val storageRef = storage.reference.child("$childEmail/iconApp")*/
        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        fun bindItem(recentApp: AppHistory, appIcon: AppIcon?) {
            with(itemView) {
                /*storageRef.child("${recentApp.namaPaketAplikasi}.png").downloadUrl
                    .addOnSuccessListener {
                        Glide.with(context)
                            .load(it)
                            .into(img_appIcon)
                    }
                    .addOnFailureListener {
                        Glide.with(context)
                            .load(R.drawable.ic_logo_test)
                            .into(img_appIcon)
                    }*/
                if (appIcon != null) {
                    Glide.with(context)
                        .load(appIcon.appIcon)
                        .into(img_appIcon)
                } else {
                    Glide.with(context)
                        .load(R.drawable.ic_home_24)
                        .into(img_appIcon)
                }
                txt_namaApp.text = recentApp.namaAplikasi
                recentApp.waktuAkses.let {
                    val sdf = SimpleDateFormat("HH:mm")
                    val netDate = Date(it)
                    txt_waktu_akses.text = "${sdf.format(netDate)} WIB"
                }
            }
        }
    }
}