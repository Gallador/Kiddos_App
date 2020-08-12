package com.dodolz.kiddos.adapter.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dodolz.kiddos.R
import com.dodolz.kiddos.model.video.SubItem
import kotlinx.android.synthetic.main.item_in_rv_video_sub.view.*

interface OnPlayClickCallback {
    fun onPlayClicked(data: SubItem)
}

class SubVideoAdapter(private val subAppList: MutableList<SubItem>)
    : RecyclerView.Adapter<SubVideoAdapter.MainListViewHolder>() {
    
    private lateinit var playClickCallback: OnPlayClickCallback
    
    fun setOnPlayClickCallback(playClick: OnPlayClickCallback) {
        this.playClickCallback = playClick
    }
    
    fun clearData() {
        subAppList.clear()
        notifyDataSetChanged()
    }
    
    fun setData(data: MutableList<SubItem>) {
        subAppList.clear()
        subAppList.addAll(data)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_in_rv_video_sub, parent, false)
        return MainListViewHolder(
            view
        )
    }
    
    override fun getItemCount(): Int {
        return subAppList.size
    }
    
    override fun onBindViewHolder(holder: MainListViewHolder, pos: Int) {
        holder.bindApp(subAppList[pos])
        holder.itemView.img_playicon.setOnClickListener{
            playClickCallback.onPlayClicked(subAppList[pos])
        }
    }
    
    class MainListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindApp(data: SubItem) {
            with(itemView) {
                txt_namaApp.text = data.appName
                txt_waktuRekam.text = data.recordTime
                /*img_playicon.setOnClickListener{
                    Toast.makeText(context, data.prefixName, Toast.LENGTH_LONG).show()
                }*/
            }
        }
    }
}