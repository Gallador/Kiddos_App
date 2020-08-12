package com.dodolz.kiddos.adapter.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dodolz.kiddos.R
import com.dodolz.kiddos.model.home.RecentApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.item_in_rv_recent_app.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RecentAppAdapter(private val childEmail: String): RecyclerView.Adapter<RecentAppAdapter.MainListViewHolder>() {
    
    private var recentAppList: ArrayList<RecentApp> = ArrayList()
    
    private fun clearData() = recentAppList.clear()
    
    fun setData(data: ArrayList<RecentApp>) {
        clearData()
        recentAppList.addAll(data)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MainListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_in_rv_recent_app, parent, false)
        return MainListViewHolder(
            view,
            childEmail
        )
    }
    
    override fun getItemCount(): Int {
        return recentAppList.size
    }
    
    override fun onBindViewHolder(holder: MainListViewHolder, position: Int) {
        holder.bindItem(recentAppList[position])
    }
    
    class MainListViewHolder(itemView: View, childEmail: String) : RecyclerView.ViewHolder(itemView) {
        private val storage = Firebase.storage
        private val storageRef = storage.reference.child("$childEmail/iconApp")
        @SuppressLint("SimpleDateFormat", "SetTextI18n")
        fun bindItem(recentApp: RecentApp) {
            with(itemView) {
                storageRef.child("${recentApp.namaPaketAplikasi}.png").downloadUrl
                    .addOnSuccessListener {
                        Glide.with(context)
                            .load(it)
                            .into(img_appIcon)
                    }
                    .addOnFailureListener {
                    // Handle any errors
                    }
                txt_namaApp.text = recentApp.namaAplikasi
                recentApp.waktuAkses?.let {
                    val sdf = SimpleDateFormat("HH:mm")
                    val netDate = Date(it)
                    txt_waktu_akses.text = "${sdf.format(netDate)} WIB"
                }
            }
        }
    }
}