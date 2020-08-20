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
import com.dodolz.kiddos.viewmodel.AppDetailViewmodel
import com.dodolz.kiddos.viewmodel.ChildSelectionStateViewmodel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_detail.*
import kotlinx.android.synthetic.main.fragment_detail.view.*

class AppDetailFragment : Fragment() {
    
    private val viewmodel: AppDetailViewmodel by activityViewModels()
    private val childSelectionStateViewmodel: ChildSelectionStateViewmodel by activityViewModels()
    private lateinit var recycleviewAdapter: DetailAppAdapter
    private lateinit var loadingDialog: MaterialDialog
    private lateinit var sortChoices: Array<String>
    private lateinit var sortChoicesAdapter: ArrayAdapter<String>
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_detail, container, false)
        view.rv_detailApps.layoutManager = LinearLayoutManager(requireContext())
        loadingDialog = MaterialDialog(requireContext())
            .title(text = "Memuat Data...")
            .message(text = "Mohon tunggu")
            .icon(R.drawable.ic_loading)
        sortChoices = arrayOf("Aplikasi", "Durasi", "Internet")
        sortChoicesAdapter = ArrayAdapter(requireContext(), R.layout.setting_dropdown_item, sortChoices)
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        childSelectionStateViewmodel.childSelected.observe(viewLifecycleOwner, Observer { childEmail ->
            loadingDialog.show()
            viewmodel.loadDetailApps(childEmail)
            txt_pilihanUrutan.apply {
                setAdapter(sortChoicesAdapter)
                setText(sortChoices[1], false)
                onItemClickListener = object: AdapterView.OnItemClickListener {
                    override fun onItemClick(p0: AdapterView<*>?, p1: View?, itemIndex: Int, p3: Long) {
                        viewmodel.changeSortData(itemIndex, childEmail)
                    }
                }
            }
        })
        viewmodel.listDetailApps.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val list = it.second
            txt_totalApp.text = "${list.size} Aplikasi"
            recycleviewAdapter = DetailAppAdapter(childEmail)
            rv_detailApps.adapter = recycleviewAdapter
            recycleviewAdapter.setData(list)
            loadingDialog.dismiss()
            requireActivity().swipeContainer.isRefreshing = false
        })
        viewmodel.changeDetailApps.observe(viewLifecycleOwner, Observer {
            recycleviewAdapter.setData(it.second)
        })
    }
    
}