package com.dodolz.kiddos.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.dodolz.kiddos.R
import com.dodolz.kiddos.adapter.setting.*
import com.dodolz.kiddos.model.setting.AppInfo
import com.dodolz.kiddos.model.setting.AppInfoForRestrict
import com.dodolz.kiddos.viewmodel.ChildSelectionStateViewmodel
import com.dodolz.kiddos.viewmodel.SettingViewmodel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_setting.*
import kotlinx.android.synthetic.main.fragment_setting.view.*
import kotlinx.android.synthetic.main.item_in_rv_setting_pembatasan.*

class SettingFragment : Fragment() {
    
    private val viewmodel: SettingViewmodel by activityViewModels()
    private val childSelectionStateViewmodel: ChildSelectionStateViewmodel by activityViewModels()
    private lateinit var activeRecordAdapter: RecordSettingAdapter
    private lateinit var nonActiveRecordAdapter: RecordSettingAdapter
    private lateinit var activeRestrictAdapter: RestrictSettingAdapter
    private lateinit var nonActiveRestrictAdapter: RecordSettingAdapter
    private lateinit var activeBlockAdapter: RecordSettingAdapter
    private lateinit var nonActiveBlockAdapter: RecordSettingAdapter
    private lateinit var loadingDialog: MaterialDialog
    private lateinit var settingChoices: Array<String>
    private lateinit var settingChoicesAdapter: ArrayAdapter<String>
    private lateinit var recordDurationChoices: Array<String>
    private lateinit var recordDurationChoicesAdapter: ArrayAdapter<String>
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_setting, container, false)
        view.rv_aktifPerekaman.layoutManager = LinearLayoutManager(requireContext())
        view.rv_aktifPerekaman.adapter = null
        view.rv_nonAktifPerekaman.layoutManager = LinearLayoutManager(requireContext())
        view.rv_nonAktifPerekaman.adapter = null
        view.rv_aktifPembatasan.layoutManager = LinearLayoutManager(requireContext())
        view.rv_aktifPembatasan.adapter = null
        view.rv_nonAktifPembatasan.layoutManager = LinearLayoutManager(requireContext())
        view.rv_nonAktifPembatasan.adapter = null
        view.rv_aktifBlokir.layoutManager = LinearLayoutManager(requireContext())
        view.rv_aktifBlokir.adapter = null
        view.rv_nonAktifBlokir.layoutManager = LinearLayoutManager(requireContext())
        view.rv_nonAktifBlokir.adapter = null
        settingChoices = arrayOf("Perekaman Layar", "Pembatasan Aplikasi", "Blokir Aplikasi")
        settingChoicesAdapter = ArrayAdapter(requireContext(), R.layout.setting_dropdown_item, settingChoices)
        recordDurationChoices = arrayOf("1 Menit", "2 Menit", "3 Menit", "4 Menit", "5 Menit")
        recordDurationChoicesAdapter = ArrayAdapter(requireContext(), R.layout.setting_dropdown_item, recordDurationChoices)
        return view
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = MaterialDialog(requireContext())
            .title(text = "Memproses")
            .message(text = "Loading")
            .icon(R.drawable.ic_loading)
        subscribeRecordObservers()
        subscribeRestrictObservers()
        subscribeBlockObservers()
        childSelectionStateViewmodel.childSelected.observe(viewLifecycleOwner, Observer {
            // Set tampilan setting ke Perekaman Layar setelah berganti anak
            showRelatedSettingView(0, it)
            loadingDialog.show()
            // Set Pilihan Setting Spinner
            txt_spinnerSetting.apply {
                setAdapter(settingChoicesAdapter)
                setText(settingChoices[0], false)
                onItemClickListener = AdapterView.OnItemClickListener { _, _, itemIndex, _ ->
                    showRelatedSettingView(itemIndex, it)
                }
            }
        })
        viewmodel.isUserRefreshing.observe(viewLifecycleOwner, Observer {
            if (it.first) refreshData(it.second)
        })
    }

    private fun refreshData(childEmail: String) {
        when (txt_spinnerSetting.text.toString()) {
            settingChoices[0] -> viewmodel.loadAppListForRecord(childEmail)
            settingChoices[1] -> viewmodel.loadAppListForRestrict(childEmail)
            settingChoices[2] -> viewmodel.loadAppListForBlock(childEmail)
        }
    }

    private fun showRelatedSettingView(itemIndex: Int, childEmail: String = "") {
        when (itemIndex) {
            0 -> {
                view_perekamanLayar.visibility = View.VISIBLE; view_pembatasanAplikasi.visibility = View.INVISIBLE; view_blokirAplikasi.visibility = View.INVISIBLE
                viewmodel.loadAppListForRecord(childEmail)
                // Set adapter for Pilihan durasi screen record
                txt_pilihanDurasi.setAdapter(recordDurationChoicesAdapter)
                txt_pilihanDurasi.onItemClickListener =
                    AdapterView.OnItemClickListener { _, _, itemIndex2, _ ->
                        setRecordDuration(itemIndex2 + 1, childEmail)
                    }
            }
            1 -> {
                view_pembatasanAplikasi.visibility = View.VISIBLE; view_perekamanLayar.visibility = View.INVISIBLE;  view_blokirAplikasi.visibility = View.INVISIBLE
                viewmodel.loadAppListForRestrict(childEmail)
            }
            2 -> {
                view_blokirAplikasi.visibility = View.VISIBLE; view_perekamanLayar.visibility = View.INVISIBLE; view_pembatasanAplikasi.visibility = View.INVISIBLE
                viewmodel.loadAppListForBlock(childEmail)
            }
        }
    }
    
    private fun subscribeRecordObservers() {
        viewmodel.recordDuration.observe(viewLifecycleOwner, Observer { recordDuration ->
            txt_pilihanDurasi.setText(recordDurationChoices[recordDuration - 1], false)
        })
        viewmodel.activeRecordList.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val activeAppList = it.second
            activeRecordAdapter =
                RecordSettingAdapter(
                    childEmail,
                    true
                )
            activeRecordAdapter.apply {
                setOnSwitchClickCallback(object:
                    OnSwitchClickCallback {
                    override fun onSwitchClicked(data: AppInfo, switch: Switch) {
                        deactivationRecordApp(data, switch, childEmail)
                    }
                })
                rv_aktifPerekaman.adapter = this
                setData(activeAppList)
            }
            loadingDialog.dismiss()
        })
        viewmodel.nonActiveRecordList.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val nonActiveAppList = it.second
            nonActiveRecordAdapter =
                RecordSettingAdapter(
                    childEmail,
                    false
                )
            nonActiveRecordAdapter.apply {
                setOnSwitchClickCallback(object:
                    OnSwitchClickCallback {
                    override fun onSwitchClicked(data: AppInfo, switch: Switch) {
                        activationRecordApp(data, switch, childEmail)
                    }
                })
                rv_nonAktifPerekaman.adapter = this
                setData(nonActiveAppList)
            }
            requireActivity().swipeContainer.isRefreshing = false
        })
    }
    
    private fun subscribeRestrictObservers() {
        viewmodel.activeRestrictList.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val activeAppList = it.second
            activeRestrictAdapter =
                RestrictSettingAdapter(
                    childEmail,
                    true
                )
            activeRestrictAdapter.apply {
                setOnSwitchClickCallback(object:
                    OnSwitchRestrictClickCallback {
                    override fun onSwitchClicked(data: AppInfoForRestrict, switch: Switch) {
                        deactivationRestrictApp(data, switch, childEmail)
                    }
                })
                setOnDropdownCLickCallback(object:
                    OnDropdownClickCallback {
                    override fun onDropdownCLicked(data: AppInfoForRestrict, itemInHour: Int) {
                        setRestrictDuration(data, itemInHour + 1, childEmail)
                    }
                })
                rv_aktifPembatasan.adapter = this
                setData(activeAppList)
            }
            loadingDialog.dismiss()
        })
        viewmodel.nonActiveRestrictList.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val nonActiveAppList = it.second
            nonActiveRestrictAdapter =
                RecordSettingAdapter(
                    childEmail,
                    false
                )
            nonActiveRestrictAdapter.apply {
                setOnSwitchClickCallback(object:
                    OnSwitchClickCallback {
                    override fun onSwitchClicked(data: AppInfo, switch: Switch) {
                        activationRestrictApp(data, switch, childEmail)
                    }
                })
                rv_nonAktifPembatasan.adapter = this
                setData(nonActiveAppList)
            }
            requireActivity().swipeContainer.isRefreshing = false
        })
    }
    
    private fun subscribeBlockObservers() {
        viewmodel.activeBlockList.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val activeAppList = it.second
            activeBlockAdapter =
                RecordSettingAdapter(
                    childEmail,
                    true
                )
            activeBlockAdapter.apply {
                setOnSwitchClickCallback(object:
                    OnSwitchClickCallback {
                    override fun onSwitchClicked(data: AppInfo, switch: Switch) {
                        deactivationBlockApp(data, switch, childEmail)
                    }
                })
                rv_aktifBlokir.adapter = this
                setData(activeAppList)
            }
            loadingDialog.dismiss()
        })
        viewmodel.nonActiveBlockList.observe(viewLifecycleOwner, Observer {
            val childEmail = it.first
            val nonActiveAppList = it.second
            nonActiveBlockAdapter =
                RecordSettingAdapter(
                    childEmail,
                    false
                )
            nonActiveBlockAdapter.apply {
                setOnSwitchClickCallback(object:
                    OnSwitchClickCallback {
                    override fun onSwitchClicked(data: AppInfo, switch: Switch) {
                        activationBlockApp(data, switch, childEmail)
                    }
                })
                rv_nonAktifBlokir.adapter = this
                setData(nonActiveAppList)
            }
            requireActivity().swipeContainer.isRefreshing = false
        })
    }
    
    private fun activationRecordApp(data: AppInfo, switch: Switch, childEmail: String) {
        MaterialDialog(requireContext()).show {
            title(text = "Aktifkan Perekaman Layar untuk aplikasi ${data.namaAplikasi}")
            message(text = "Perekaman Layar akan diaktifkan ketika anak sedang membuka aplikasi ${data.namaAplikasi}")
            positiveButton(text = "Lanjutkan") {
                loadingDialog.show()
                viewmodel.activateAppRecord(data, childEmail)
                viewmodel.activationRecordStatus.observe(viewLifecycleOwner, Observer { result ->
                    if (result) {
                        loadingDialog.dismiss()
                        activeRecordAdapter.clearData()
                        nonActiveRecordAdapter.clearData()
                        Toast.makeText(
                            requireContext(),
                            "Pengaktifan Perekaman Layar untuk aplikasi ${data.namaAplikasi} berhasil.",
                            Toast.LENGTH_LONG).show()
                        viewmodel.loadAppListForRecord(childEmail, true)
                    } else {
                        loadingDialog.dismiss()
                        switch.isChecked = true
                        Toast.makeText(
                            requireContext(),
                            "Pengaktifan Perekaman Layar untuk aplikasi ${data.namaAplikasi} gagal.",
                            Toast.LENGTH_LONG).show()
                    }
                })
            }
            negativeButton(text = "Batalkan") {
                switch.isChecked = false
            }
        }
    }
    
    private fun deactivationRecordApp(data: AppInfo, switch: Switch, childEmail: String) {
        MaterialDialog(requireContext()).show {
            title(text = "Non Aktifkan Perekaman Layar untuk aplikasi ${data.namaAplikasi}")
            message(text = "Perekaman layar akan dinon-aktifkan untuk aplikasi ${data.namaAplikasi}")
            positiveButton(text = "Lanjutkan") {
                loadingDialog.show()
                data.namaAplikasi?.let { namaAplikasi ->
                    viewmodel.deactivateAppRecord(namaAplikasi, childEmail)
                    viewmodel.deactivationRecordStatus.observe(viewLifecycleOwner, Observer { result ->
                        if (result) {
                            loadingDialog.dismiss()
                            activeRecordAdapter.clearData()
                            nonActiveRecordAdapter.clearData()
                            Toast.makeText(
                                requireContext(),
                                "Penonaktifan Perekaman Layar untuk aplikasi $namaAplikasi berhasil.",
                                Toast.LENGTH_LONG).show()
                            viewmodel.loadAppListForRecord(childEmail, true)
                        } else {
                            loadingDialog.dismiss()
                            switch.isChecked = true
                            Toast.makeText(
                                requireContext(),
                                "Penonaktifan Perekaman Layar untuk aplikasi $namaAplikasi gagal.",
                                Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
            negativeButton(text = "Batalkan") {
                switch.isChecked = true
            }
        }
    }
    
    private fun setRecordDuration(minute: Int, childEmail: String) {
        loadingDialog.show()
        viewmodel.setRecordDuration(minute, childEmail)
        viewmodel.setRecordDurationStatus.observe(viewLifecycleOwner, Observer {
            if (it) {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Durasi Perekaman Layar berhasil disimpan", Toast.LENGTH_LONG).show()
            } else {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Durasi Perekaman Layar gagal disimpan", Toast.LENGTH_LONG).show()
                txt_spinnerSetting.setText(recordDurationChoices[minute - 1], false)
            }
        })
    }
    
    private fun activationRestrictApp(data: AppInfo, switch: Switch, childEmail: String) {
        MaterialDialog(requireContext()).show {
            title(text = "Aktifkan Pembatasan Penggunaan untuk aplikasi ${data.namaAplikasi}")
            message(text = "Pembatasan Penggunaan akan diaktifkan untuk aplikasi ${data.namaAplikasi}")
            positiveButton(text = "Lanjutkan") {
                loadingDialog.show()
                viewmodel.activateAppRestrict(data, childEmail)
                viewmodel.activationRestrictStatus.observe(viewLifecycleOwner, Observer { result ->
                    if (result) {
                        loadingDialog.dismiss()
                        activeRestrictAdapter.clearData()
                        nonActiveRestrictAdapter.clearData()
                        Toast.makeText(
                            requireContext(),
                            "Aktivasi Pembatasan Penggunaan untuk aplikasi ${data.namaAplikasi} berhasil.",
                            Toast.LENGTH_LONG).show()
                        viewmodel.loadAppListForRestrict(childEmail, true)
                    } else {
                        loadingDialog.dismiss()
                        switch.isChecked = true
                        Toast.makeText(
                            requireContext(),
                            "Aktivasi Pembatasan Penggunaan untuk aplikasi ${data.namaAplikasi} gagal.",
                            Toast.LENGTH_LONG).show()
                    }
                })
            }
            negativeButton(text = "Batalkan") {
                switch.isChecked = false
            }
        }
    }
    
    private fun deactivationRestrictApp(data: AppInfoForRestrict, switch: Switch, childEmail: String) {
        MaterialDialog(requireContext()).show {
            title(text = "Non Aktifkan Pembatasan Penggunaan untuk aplikasi ${data.namaAplikasi}")
            message(text = "Pembatasan Penggunaan akan dinon-aktifkan untuk aplikasi ${data.namaAplikasi}")
            positiveButton(text = "Lanjutkan") {
                loadingDialog.show()
                data.namaAplikasi?.let { namaAplikasi ->
                    viewmodel.deactivateAppRestrict(namaAplikasi, childEmail)
                    viewmodel.deactivationRestrictStatus.observe(viewLifecycleOwner, Observer { result ->
                        if (result) {
                            loadingDialog.dismiss()
                            activeRestrictAdapter.clearData()
                            nonActiveRestrictAdapter.clearData()
                            Toast.makeText(
                                requireContext(),
                                "Penonaktifan Pembatasan Penggunaan untuk aplikasi $namaAplikasi berhasil.",
                                Toast.LENGTH_LONG).show()
                            viewmodel.loadAppListForRestrict(childEmail, true)
                        } else {
                            loadingDialog.dismiss()
                            switch.isChecked = true
                            Toast.makeText(
                                requireContext(),
                                "Penonaktifan Pembatasan Penggunaan untuk aplikasi $namaAplikasi gagal.",
                                Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
            negativeButton(text = "Batalkan") {
                switch.isChecked = true
            }
        }
    }
    
    private fun setRestrictDuration(data: AppInfoForRestrict, hour: Int, childEmail: String) {
        loadingDialog.show()
        viewmodel.setRestrictDuration(data, hour, childEmail)
        viewmodel.setRestrictDurationStatus.observe(viewLifecycleOwner, Observer {
            if (it) {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Durasi Pembatasan Penggunaan untuk Aplikasi ${data.namaAplikasi} berhasil disimpan", Toast.LENGTH_LONG).show()
            } else {
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Durasi Perekaman Layar gagal disimpan", Toast.LENGTH_LONG).show()
                txt_pilihanDurasiPembatasan.setText(recordDurationChoices[hour - 1], false)
            }
        })
    }
    
    private fun activationBlockApp(data: AppInfo, switch: Switch, childEmail: String) {
        MaterialDialog(requireContext()).show {
            title(text = "Aktifkan Blokir akses untuk aplikasi ${data.namaAplikasi}")
            message(text = "Blokir akan diaktifkan untuk aplikasi ${data.namaAplikasi}")
            positiveButton(text = "Lanjutkan") {
                loadingDialog.show()
                viewmodel.activateAppBlock(data, childEmail)
                viewmodel.activationBlockStatus.observe(viewLifecycleOwner, Observer { result ->
                    if (result) {
                        loadingDialog.dismiss()
                        activeBlockAdapter.clearData()
                        nonActiveBlockAdapter.clearData()
                        Toast.makeText(
                            requireContext(),
                            "Aktivasi pemblokiran untuk aplikasi ${data.namaAplikasi} berhasil.",
                            Toast.LENGTH_LONG).show()
                        viewmodel.loadAppListForBlock(childEmail, true)
                    } else {
                        loadingDialog.dismiss()
                        switch.isChecked = true
                        Toast.makeText(
                            requireContext(),
                            "Aktivasi pemblokiran untuk aplikasi ${data.namaAplikasi} gagal.",
                            Toast.LENGTH_LONG).show()
                    }
                })
            }
            negativeButton(text = "Batalkan") {
                switch.isChecked = false
            }
        }
    }
    
    private fun deactivationBlockApp(data: AppInfo, switch: Switch, childEmail: String) {
        MaterialDialog(requireContext()).show {
            title(text = "Non Aktifkan Blokir akses untuk aplikasi ${data.namaAplikasi}")
            message(text = "Blokir akan dinon-aktifkan untuk aplikasi ${data.namaAplikasi}")
            positiveButton(text = "Lanjutkan") {
                loadingDialog.show()
                data.namaAplikasi?.let { namaAplikasi ->
                    viewmodel.deactivateAppBlock(namaAplikasi, childEmail)
                    viewmodel.deactivationBlockStatus.observe(viewLifecycleOwner, Observer { result ->
                        if (result) {
                            loadingDialog.dismiss()
                            activeBlockAdapter.clearData()
                            nonActiveBlockAdapter.clearData()
                            viewmodel.loadAppListForBlock(childEmail, true)
                            Toast.makeText(
                                requireContext(),
                                "Penonaktifan Blokir akses untuk aplikasi $namaAplikasi berhasil.",
                                Toast.LENGTH_LONG).show()
                        } else {
                            loadingDialog.dismiss()
                            switch.isChecked = true
                            Toast.makeText(
                                requireContext(),
                                "Penonaktifan Blokir akses untuk aplikasi $namaAplikasi gagal.",
                                Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
            negativeButton(text = "Batalkan") {
                switch.isChecked = true
            }
        }
    }
}