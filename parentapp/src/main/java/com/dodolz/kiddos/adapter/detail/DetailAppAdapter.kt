package com.dodolz.kiddos.adapter.detail

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dodolz.kiddos.R
import com.dodolz.kiddos.model.detail.DetailApp
import com.dodolz.kiddos.utils.ConvertByte
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.item_in_rv_detail_app.view.*
import java.util.concurrent.TimeUnit


class DetailAppAdapter(private val childEmail: String): RecyclerView.Adapter<DetailAppAdapter.MainListViewHolder>() {
    
    private var appList: MutableList<DetailApp> = mutableListOf()
    
    fun setData(data: MutableList<DetailApp>) {
        appList = data
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MainListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_in_rv_detail_app, parent, false)
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
        fun bindItem(data: DetailApp) {
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
                data.durasiPenggunaan?.let {
                    val hour = TimeUnit.MILLISECONDS.toHours(it)
                    val minute = TimeUnit.MILLISECONDS.toMinutes(it) % TimeUnit.HOURS.toMinutes(1)
                    val teks = if (hour == 0L) {
                        "$minute Menit"
                    } else {
                        "$hour Jam $minute Menit"
                    }
                    txt_durasi.text = teks
                }
                data.penggunaanInternet?.let {
                    val internet = ConvertByte.getSize(it)
                    txt_internet.text = internet
                }
            }
        }
    }
}