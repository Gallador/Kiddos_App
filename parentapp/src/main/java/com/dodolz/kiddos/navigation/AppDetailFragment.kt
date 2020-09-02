package com.dodolz.kiddos.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.dodolz.kiddos.R
import com.dodolz.kiddos.adapter.detail.DetailAppAdapter
import com.dodolz.kiddos.adapter.detail.UninstalledAppAdapter
import com.dodolz.kiddos.viewmodel.AppDetailViewmodel
import com.dodolz.kiddos.viewmodel.ChildSelectionStateViewmodel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_detail.*
import kotlinx.android.synthetic.main.fragment_detail.view.*

class AppDetailFragment : Fragment() {
    
    private val viewmodel: AppDetailViewmodel by activityViewModels()
    private val childSelectionStateViewmodel: ChildSelectionStateViewmodel by activityViewModels()
    private lateinit var usageRvAdapter: DetailAppAdapter
    private lateinit var uninstalledRvAdapter: UninstalledAppAdapter
    private lateinit var loadingDialog: MaterialDialog
    private lateinit var menuChoices: Array<String>
    private lateinit var menuChoicesAdapter: ArrayAdapter<String>
    private lateinit var sortChoices: Array<String>
    private lateinit var sortChoicesAdapter: ArrayAdapter<String>
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_detail, container, false)
        view.rv_detailApps.layoutManager = LinearLayoutManager(requireContext())
        view.rv_uninstalledApps.layoutManager = LinearLayoutManager(requireContext())
        loadingDialog = MaterialDialog(requireContext())
            .title(text = "Memuat Data...")
            .message(text = "Mohon tunggu")
            .icon(R.drawable.ic_loading)
        menuChoices = arrayOf("Penggunaan Aplikasi", "Aplikasi Dihapus")
        menuChoicesAdapter = ArrayAdapter(requireContext(), R.layout.setting_dropdown_item, menuChoices)
        sortChoices = arrayOf("Aplikasi", "Durasi", "Internet")
        sortChoicesAdapter = ArrayAdapter(requireContext(), R.layout.setting_dropdown_item, sortChoices)
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        childSelectionStateViewmodel.childSelected.observe(viewLifecycleOwner, Observer { childEmail ->
            loadingDialog.show()
            viewmodel.loadDetailApps(childEmail)
            txt_spinnerAplikasi.apply {
                setAdapter(menuChoicesAdapter)
                setText(menuChoices[0], false)
                onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, itemIndex, _ ->
                        loadingDialog.show()
                        when (itemIndex) {
                            0 -> { viewmodel.loadDetailApps(childEmail) }
                            1 -> { viewmodel.getUnistalledApps(childEmail) }
                        }
                    }
            }
            txt_pilihanUrutan.apply {
                setAdapter(sortChoicesAdapter)
                setText(sortChoices[1], false)
                onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, itemIndex, _ ->
                        viewmodel.changeSortData(itemIndex, childEmail)
                    }
            }
        })
        viewmodel.listDetailApps.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val list = it.second
            container_usage.visibility = View.VISIBLE
            container_uninstalled.visibility = View.INVISIBLE
            txt_totalApp.text = "${list.size} Aplikasi"
            usageRvAdapter = DetailAppAdapter(childEmail)
            rv_detailApps.adapter = usageRvAdapter
            usageRvAdapter.setData(list)
            loadingDialog.dismiss()
            requireActivity().swipeContainer.isRefreshing = false
        })
        viewmodel.changeDetailApps.observe(viewLifecycleOwner, Observer {
            usageRvAdapter.setData(it.second)
        })
        viewmodel.uninstalledApps.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val list = it.second
            container_usage.visibility = View.INVISIBLE
            container_uninstalled.visibility = View.VISIBLE
            uninstalledRvAdapter = UninstalledAppAdapter(childEmail)
            rv_uninstalledApps.adapter = uninstalledRvAdapter
            uninstalledRvAdapter.setData(list)
            loadingDialog.dismiss()
            requireActivity().swipeContainer.isRefreshing = false
        })
    }
}