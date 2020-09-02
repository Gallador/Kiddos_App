package com.dodolz.kiddos.adapter.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dodolz.kiddos.R
import com.dodolz.kiddos.model.detail.UninstalledApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.item_in_rv_uninstalled_app.view.*
import java.text.SimpleDateFormat
import java.util.*

class UninstalledAppAdapter(private val childEmail: String): RecyclerView.Adapter<UninstalledAppAdapter.MainListViewHolder>() {
    
    private var appList: MutableList<UninstalledApp> = mutableListOf()
    
    fun setData(data: MutableList<UninstalledApp>) {
        appList = data
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MainListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_in_rv_uninstalled_app, parent, false)
        return MainListViewHolder(
            view,
            childEmail
        )
    }
    
    override fun getItemCount(): Int {
        return appList.size
    }
    
    override fun onBindViewHolder(holder: MainListViewHolder, position: Int) {
        holder.bindItem(appList[position])
    }
    
    class MainListViewHolder(itemView: View, childEmail: String) : RecyclerView.ViewHolder(itemView) {
        private val storage = Firebase.storage
        private val storageRef = storage.reference.child("$childEmail/iconApp")
        @SuppressLint("DefaultLocale")
        fun bindItem(data: UninstalledApp) {
            with(itemView) {
                storageRef.child("${data.namaPaketAplikasi}.png").downloadUrl
                    .addOnSuccessListener {
                        Glide.with(context)
                            .load(it)
                            .into(img_appIcon)
                    }
                    .addOnFailureListener {
                    // Handle any errors
                    }
                txt_namaApp.text = data.namaAplikasi
                data.waktuHapus?.let {
                    txt_waktuHapus.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(System.currentTimeMillis()))
                }
            }
        }
    }
}