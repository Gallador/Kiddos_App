package com.dodolz.kiddos.navigation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.dodolz.kiddos.R
import com.dodolz.kiddos.adapter.video.MainVideoAdapter
import com.dodolz.kiddos.adapter.video.OnSubItemClickCallback
import com.dodolz.kiddos.model.video.SubItem
import com.dodolz.kiddos.viewmodel.ChildSelectionStateViewmodel
import com.dodolz.kiddos.viewmodel.VideoViewmodel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_video.*
import kotlinx.android.synthetic.main.fragment_video.view.*
import java.io.File


class VideoFragment : Fragment() {
    
    private val viewmodel: VideoViewmodel by activityViewModels()
    private val childSelectionStateViewmodel: ChildSelectionStateViewmodel by activityViewModels()
    private lateinit var videoAdapter: MainVideoAdapter
    private lateinit var loadingDialog: MaterialDialog
    private lateinit var requestDialog: MaterialDialog
    private lateinit var waitingDialog: MaterialDialog
    private lateinit var downloadDialog: MaterialDialog
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_video, container, false)
        view.rv_mainItem.layoutManager = LinearLayoutManager(view.context)
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = MaterialDialog(view.context)
            .title(text = "Memuat Data...")
            .message(text = "Mohon tunggu")
            .icon(R.drawable.ic_loading)
        requestDialog = MaterialDialog(view.context)
            .title(text = "Mengirim Permintaan Pemrosesan Video Ke Server")
            .message(text = "Mohon tunggu")
            .cancelOnTouchOutside(false)
            .icon(R.drawable.ic_loading)
        waitingDialog = MaterialDialog(view.context)
            .title(text = "Menunggu Pemrosesan Video")
            .message(text = "Mohon tunggu")
            .cancelOnTouchOutside(false)
            .icon(R.drawable.ic_loading)
        downloadDialog = MaterialDialog(view.context)
            .title(text = "Sedang Mengunduh Video")
            .message(text = "Mohon tunggu")
            .cancelOnTouchOutside(false)
            .icon(R.drawable.ic_loading)
        
        childSelectionStateViewmodel.childSelected.observe(viewLifecycleOwner, Observer { childEmail ->
            viewmodel.getListOfVideos(childEmail)
            loadingDialog.show()
        })
        viewmodel.listOfVideo.observe(viewLifecycleOwner, Observer {
            txt_belumAdaVideo.visibility = View.INVISIBLE
            loadingDialog.dismiss()
            val childEmail = it.first
            val videoList = it.second
            if (videoList.size == 0) {
                txt_belumAdaVideo.visibility = View.VISIBLE
            }
            videoAdapter = MainVideoAdapter(videoList)
            videoAdapter.setOnSubItemClickCallback(object : OnSubItemClickCallback {
                override fun onSubItemClicked(data: SubItem) {
                    sendRequestForVideo(data, childEmail)
                }
            })
            rv_mainItem.adapter = videoAdapter
        })
        viewmodel.requestStatus.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val videoInfo = it.second
            when (videoInfo.reqStatus) {
                "downloading" -> {
                    requestDialog.dismiss()
                    downloadDialog.show()
                    viewmodel.downloadVideo(childEmail, videoInfo.reqID, videoInfo.downloadURL)
                }
                "waiting" -> {
                    requestDialog.dismiss()
                    waitingDialog.show()
                    viewmodel.watchProcessedVideo(childEmail, videoInfo.reqID)
                }
                else -> {
                    requestDialog.dismiss()
                    Toast.makeText(view.context, "Permintaan pemrosesan video ke server gagal. Coba ulangi secara berkala",
                    Toast.LENGTH_LONG).show()
                }
            }
        })
        viewmodel.downloadUrlReady.observe(viewLifecycleOwner, Observer {
            waitingDialog.dismiss()
            downloadDialog.show()
            val childEmail = it.first
            val videoInfo = it.second
            if (videoInfo.downloadURL.isNotEmpty()) {
                viewmodel.downloadVideo(childEmail, videoInfo.reqID, videoInfo.downloadURL)
            }
        })
        viewmodel.videoPath.observe(viewLifecycleOwner, Observer {
            if (it.isNotEmpty() && it != "fail") {
                downloadDialog.dismiss()
                requestDialog.dismiss()
                launchActionPlayVideo(view.context, it)
            } else {
                Toast.makeText(view.context, "Pengunduhan video gagal. Coba ulangi beberapa saat lagi",
                    Toast.LENGTH_LONG).show()
            }
        })
    }
    
    private fun sendRequestForVideo(data: SubItem, childEmail: String) {
        requestDialog.show()
        FirebaseAuth.getInstance().currentUser?.uid?.let { viewmodel.postRequestForVideo(it, data, childEmail) }
    }
    
    private fun launchActionPlayVideo(context: Context, videoPath: String) {
        val videoFile = File(videoPath)
        val fileUri =
            FileProvider.getUriForFile(context, "com.dodolz.kiddos.fileprovider", videoFile)
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, "video/mp4")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) //DO NOT FORGET THIS EVER
        startActivity(intent)
    }
    
}