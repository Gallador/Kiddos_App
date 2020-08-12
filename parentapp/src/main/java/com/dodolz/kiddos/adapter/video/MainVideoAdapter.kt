package com.dodolz.kiddos.adapter.video

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dodolz.kiddos.R
import com.dodolz.kiddos.model.video.MainItem
import com.dodolz.kiddos.model.video.SubItem
import kotlinx.android.synthetic.main.item_in_rv_video_main.view.*

interface OnSubItemClickCallback{
    fun onSubItemClicked(data: SubItem)
}

class MainVideoAdapter(private val mainAppList: MutableList<MainItem>)
    : RecyclerView.Adapter<MainVideoAdapter.MainListViewHolder>() {
    
    private val viewPool = RecyclerView.RecycledViewPool()
    private lateinit var subItemClickCallback: OnSubItemClickCallback
    
    fun setOnSubItemClickCallback(subItemClick: OnSubItemClickCallback) {
        this.subItemClickCallback = subItemClick
    }
    
    fun clearData() {
        mainAppList.clear()
        notifyDataSetChanged()
    }
    
    fun setData(data: MutableList<MainItem>) {
        mainAppList.clear()
        mainAppList.addAll(data)
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_in_rv_video_main, parent, false)
        return MainListViewHolder(
            view
        )
    }
    
    override fun getItemCount(): Int {
        return mainAppList.size
    }
    
    override fun onBindViewHolder(holder: MainListViewHolder, pos: Int) {
        holder.bindApp(mainAppList[pos])
        val childLayoutManager = LinearLayoutManager(holder.itemView.rv_subItem.context)
        val subAdapter = SubVideoAdapter(mainAppList[pos].subList)
        subAdapter.setOnPlayClickCallback(object: OnPlayClickCallback{
            override fun onPlayClicked(data: SubItem) {
                subItemClickCallback.onSubItemClicked(data)
            }
        })
        holder.itemView.rv_subItem.apply {
            layoutManager = childLayoutManager
            adapter = subAdapter
            setRecycledViewPool(viewPool)
        }
    }
    
    class MainListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindApp(data: MainItem) {
            with(itemView) {
                txt_hari.text = data.dateNormal
            }
        }
    }
}