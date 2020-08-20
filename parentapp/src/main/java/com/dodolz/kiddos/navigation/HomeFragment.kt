package com.dodolz.kiddos.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.dodolz.kiddos.R
import com.dodolz.kiddos.adapter.home.RecentAppAdapter
import com.dodolz.kiddos.model.home.ChildInfo
import com.dodolz.kiddos.model.home.RecentApp
import com.dodolz.kiddos.utils.ConvertByte
import com.dodolz.kiddos.viewmodel.ChildSelectionStateViewmodel
import com.dodolz.kiddos.viewmodel.HomeViewmodel
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {
    
    private val viewmodel: HomeViewmodel by activityViewModels()
    private val childSelectionStateViewmodel: ChildSelectionStateViewmodel by activityViewModels()
    private lateinit var recycleviewAdapter: RecentAppAdapter
    private lateinit var loadingDialog: MaterialDialog
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = MaterialDialog(view.context)
            .title(text = "Memuat Data...")
            .message(text = "Mohon tunggu")
            .icon(R.drawable.ic_loading)
        view.rv_recentApps.layoutManager = LinearLayoutManager(context)
        view.rv_recentApps.isNestedScrollingEnabled = false
        subscribeObservers(view.context)
    }
    
    private fun subscribeObservers(context: Context) {
        childSelectionStateViewmodel.childSelected.observe(viewLifecycleOwner, Observer {
            loadingDialog.show()
            viewmodel.loadChildrenUsageSum(it)
            viewmodel.loadChildrenRecentApp(it)
        })
        viewmodel.usageSum.observe(viewLifecycleOwner, Observer{ updateUsageSum(it, context) })
        viewmodel.recentApps.observe(viewLifecycleOwner, Observer { updateRecentApp(it) })
    }
    
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun updateUsageSum(data: ChildInfo, context: Context) {
        // Download app icon for appPalingLamaDiakses from child directory in storage
        data.timestampPemutakhiranData?.let {
            val timestamp: Long = it.toString().toLong()
            val sdf = SimpleDateFormat("HH:mm")
            val netDate = Date(timestamp)
            txt_waktuDimutakhirkan.text = "Dimutakhirkan ${sdf.format(netDate)}"
        }
        val storage = Firebase.storage
        val storageRef = storage.reference.child("${data.email}/iconApp")
        storageRef.child("${data.appPalingLamaDiakses?.get(1).toString()}.png").downloadUrl
            .addOnSuccessListener { iconUrl ->
                iconUrl?.let {
                    Glide.with(requireContext())
                        .load(iconUrl)
                        .into(img_appPalingLama)
                }
            }
            .addOnFailureListener {
                Glide.with(requireContext())
                    .load(R.drawable.ic_home_24)
                    .into(img_appPalingLama)
            }
        //--------------------------------------

        data.appPalingLamaDiakses?.let {
            txt_appPalingLama.text = it[0]
        }
        data.totalDurasiPenggunaanSmartphone?.let {
            val hour = TimeUnit.MILLISECONDS.toHours(it)
            val minute = TimeUnit.MILLISECONDS.toMinutes(it) % TimeUnit.HOURS.toMinutes(1)
            val teks = if (hour == 0L) {
                "$minute Menit"
            } else {
                "$hour Jam $minute Menit"
            }
            txt_durasiPenggunaan.text = teks
        }
        
        data.totalPenggunaanInternet?.let {
            txt_penggunaanInternet.text = ConvertByte.getSize(it)
        }
    }
    
    private fun updateRecentApp(data: Pair<String, ArrayList<RecentApp>?>) {
        loadingDialog.dismiss()
        container_root.smoothScrollTo(0, 0, 500)
        val childEmail = data.first
        data.second?.let {
            // childEmail: String for access child directory in storage to load appIcon
            recycleviewAdapter =
                RecentAppAdapter(childEmail)
            rv_recentApps.adapter = recycleviewAdapter
            recycleviewAdapter.setData(it)
        }
        requireActivity().swipeContainer.isRefreshing = false
    }
}