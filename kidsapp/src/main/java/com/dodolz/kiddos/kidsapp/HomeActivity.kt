@file:Suppress("DEPRECATION")

package com.dodolz.kiddos.kidsapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.LOCATION_REQUEST
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.MEDIA_PROJECTION_DATA
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.MEDIA_PROJECTION_RESULT_CODE
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.SCREEN_RECORD_REQUEST_CODE
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.SUB_ID
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.USAGE_PERMS_REQUEST_CODE
import com.dodolz.kiddos.kidsapp.ConstantValue.Companion.USER_EMAIL
import com.dodolz.kiddos.kidsapp.activity.EditProfileActivity
import com.dodolz.kiddos.kidsapp.activity.KidsVerificationActivity
import com.dodolz.kiddos.kidsapp.activity.LoginActivity
import com.dodolz.kiddos.kidsapp.adapter.RecentAppAdapter
import com.dodolz.kiddos.kidsapp.model.AppIcon
import com.dodolz.kiddos.kidsapp.model.PhoneUsage
import com.dodolz.kiddos.kidsapp.service.MainForegroundService
import com.dodolz.kiddos.kidsapp.util.ConvertByte
import com.dodolz.kiddos.kidsapp.viewmodel.MainViewmodel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.header_side_drawer.view.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

class HomeActivity : AppCompatActivity() {
    
    private lateinit var viewmodel: MainViewmodel
    private lateinit var recycleviewAdapter: RecentAppAdapter
    private val perms = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    //private val ALL_PERMISSION = 124
    
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            drawer_layout.openDrawer(GravityCompat.START)
            checkService()
        }
        viewmodel = ViewModelProvider(this).get(MainViewmodel::class.java)
        
        rv_recentApps.layoutManager = LinearLayoutManager(this)
        rv_recentApps.isNestedScrollingEnabled = false
        
        viewmodel.phoneUsage.observe(this, contentObserver)
        
        side_nav_view.setNavigationItemSelectedListener { item ->
            return@setNavigationItemSelectedListener when (item.itemId) {
                R.id.edit_profile -> {
                    startActivity(Intent(this@HomeActivity, EditProfileActivity::class.java))
                    true
                }
                R.id.verifikasi -> {
                    startActivity(Intent(this@HomeActivity, KidsVerificationActivity::class.java))
                    true
                }
                R.id.signout -> {
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this@HomeActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finishAndRemoveTask()
                    true
                }
                else -> false
            }
        }
        askingPermission()
    }
    
    private fun checkUsageAccessPermission(): Boolean {
        return try {
            val packageManager = this.packageManager
            val applicationInfo = packageManager.getApplicationInfo(this.packageName, 0)
            val appOpsManager = this.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid,
                applicationInfo.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
    
    @AfterPermissionGranted(123)
    private fun askingPermission() {
        if (EasyPermissions.hasPermissions(this, *perms)) {
            if (checkUsageAccessPermission()) {
                // Start Load and Send Info to DB and start Foreground Service
                val manager =
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val screenCaptureIntent = manager.createScreenCaptureIntent()
                startActivityForResult(screenCaptureIntent, SCREEN_RECORD_REQUEST_CODE)
            } else {
                MaterialDialog(this).show {
                    title(text = "Izin Akses Perangkat")
                    message(text = "Izinkan Aplikasi untuk mengakses riwayat penggunaan perangkat untuk menggunakan aplikasi")
                    positiveButton(text = "Lanjutkan") {
                        startActivityForResult(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), USAGE_PERMS_REQUEST_CODE)
                    }
                    negativeButton(text = "Batalkan") {
                        this.dismiss()
                    }
                }
            }
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this@HomeActivity, this.resources.getString(R.string.permission), 123, *perms)
        }
    }
    
    @SuppressLint("SetTextI18n", "HardwareIds")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                progressbar.visibility = View.VISIBLE
                txt_memuatData.text = this.resources.getString(R.string.sedang_memuat_data)
                val subscriberId = (this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).subscriberId
                Intent(this, MainForegroundService::class.java).also { intent ->
                    intent.putExtra(MEDIA_PROJECTION_DATA, data)
                    intent.putExtra(MEDIA_PROJECTION_RESULT_CODE, resultCode)
                    intent.putExtra(USER_EMAIL, FirebaseAuth.getInstance().currentUser?.email)
                    intent.putExtra(SUB_ID, subscriberId)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent)
                    else startService(intent)
                }
                FirebaseAuth.getInstance().currentUser?.let {
                    viewmodel.getPhoneUsageStats(it.email.toString(), subscriberId)
                    txt_greeting.text = "Hai, ${it.displayName.toString()}"
                    val headerView = side_nav_view.getHeaderView(0)
                    headerView.txt_email.text = it.email.toString()
                    headerView.txt_name.text = it.displayName.toString()
                    it.photoUrl?.let { profilePicUrl ->
                        Glide.with(this@HomeActivity)
                            .load(profilePicUrl)
                            .into(headerView.img_profilePic)
                    }
                    getLocation(it.email.toString())
                }
            }
        } else if (requestCode == USAGE_PERMS_REQUEST_CODE) {
            if (checkUsageAccessPermission()) {
                askingPermission()
            }
        } else if (requestCode == LOCATION_REQUEST) {
            FirebaseAuth.getInstance().currentUser?.let {
                getLocation(it.email.toString())
            }
        }
    }
    
    private val contentObserver = Observer<Triple<String, PhoneUsage, Map<String, AppIcon>>> { data ->
        progressbar.visibility = View.INVISIBLE
        txt_memuatData.visibility = View.INVISIBLE
        container_main.visibility = View.VISIBLE
        val usageSum = data.second.usageSummary
        val recentApps = data.second.mapOfAppHistory
        val appIcon = data.third
        val bitmap = appIcon[usageSum.appPalingLamaDiakses[1]]
        if (bitmap != null) {
            Glide.with(this@HomeActivity)
                .load(bitmap.appIcon)
                .into(img_appPalingLama)
        } else {
            Glide.with(this@HomeActivity)
                .load(R.drawable.ic_home_24)
                .into(img_appPalingLama)
        }
        txt_appPalingLama.text = usageSum.appPalingLamaDiakses[0]
        usageSum.totalDurasiPenggunaanSmartphone.let {
            val hour = TimeUnit.MILLISECONDS.toHours(it)
            val minute = TimeUnit.MILLISECONDS.toMinutes(it) % TimeUnit.HOURS.toMinutes(1)
            val teks = if (hour == 0L) {
                "$minute Menit"
            } else {
                "$hour Jam $minute Menit"
            }
            txt_durasiPenggunaan.text = teks
        }
        txt_penggunaanInternet.text = ConvertByte.getSize(usageSum.totalPenggunaanInternet)
        recycleviewAdapter = RecentAppAdapter(recentApps.values.toList(), appIcon)
        rv_recentApps.adapter = recycleviewAdapter
    }
    
    @SuppressLint("MissingPermission")
    fun getLocation(email: String) {
        val locationManager: LocationManager? = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsStatus = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        if (gpsStatus) {
            val locListener = object : LocationListener {
                override fun onLocationChanged(location: Location?) {
                    location?.also {
                        /*viewmodel.sendLocation(it.latitude, it.longitude, email)
                        locationManager?.removeUpdates(this)*/
                        val mFusedLocation = LocationServices.getFusedLocationProviderClient(this@HomeActivity)
                        mFusedLocation.lastLocation.addOnSuccessListener(this@HomeActivity) {
                            val lat = it.latitude
                            val long = it.longitude
                            viewmodel.sendLocation(lat, long, email)
                            locationManager?.removeUpdates(this)
                        }
                    }
                }
                override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {}
                override fun onProviderEnabled(p0: String?) {}
                override fun onProviderDisabled(p0: String?) {}
            }
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
                0f, locListener)
        } else {
            MaterialDialog(this).show {
                title(text = "Aktifkan Lokasi")
                message(text = "Lokasi dibutuhkan untuk menggunakan aplikasi")
                positiveButton(text = "Lanjutkan") {
                    startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), LOCATION_REQUEST)
                }
                negativeButton(text = "Batalkan") {
                    this.dismiss()
                }
            }
        }
    }
    
    private fun checkService(){
        val myAM: ActivityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val myList: List<ActivityManager.RunningServiceInfo> = myAM.getRunningServices(30)
        for (element in myList) {
            val serviceName = MainForegroundService::class.java.name
            val mName: String = element.service.className
            val sa = element.service.packageName
            val aaa = element.service.shortClassName
            Log.d("SERVICE NAME", "$serviceName - $mName - $sa - $aaa")
        }
    }
}
