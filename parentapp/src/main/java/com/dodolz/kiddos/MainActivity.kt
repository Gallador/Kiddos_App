package com.dodolz.kiddos

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.get
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.androidnetworking.AndroidNetworking
import com.bumptech.glide.Glide
import com.dodolz.kiddos.activity.LoginActivity
import com.dodolz.kiddos.viewmodel.*
import com.downloader.PRDownloader
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header_side_drawer.*
import kotlinx.android.synthetic.main.header_side_drawer.view.*
import kotlinx.android.synthetic.main.item_child_selector.view.*
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), NavController.OnDestinationChangedListener{

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var homeViewmodel: HomeViewmodel
    private lateinit var appDetailViewmodel: AppDetailViewmodel
    private lateinit var videoViewmodel: VideoViewmodel
    private lateinit var settingViewmodel: SettingViewmodel
    private lateinit var locationViewmodel: LocationViewmodel
    // This viewmodel will be subscribed by all fragment in bottom nav view
    private lateinit var childSelectionStateViewmodel: ChildSelectionStateViewmodel
    
    private inline fun <VM : ViewModel> viewModelFactory(crossinline f: () -> VM) =
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(aClass: Class<T>):T = f() as T
        }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val okHttpClient: OkHttpClient = OkHttpClient().newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
        AndroidNetworking.initialize(applicationContext, okHttpClient)
        PRDownloader.initialize(applicationContext)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)
        bottomNav = findViewById(R.id.bottom_nav)
        navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_app_detail,
                R.id.nav_video,
                R.id.nav_setting,
                R.id.nav_location,
                R.id.nav_edit_profile,
                R.id.nav_add_child
            ), drawer_layout
        )
        navController.addOnDestinationChangedListener(this)
        setupActionBarWithNavController(navController, appBarConfiguration)
        bottomNav.setupWithNavController(navController)
        side_nav_view.setupWithNavController(navController)
        toolbar.setNavigationOnClickListener {
            drawer_layout.openDrawer(GravityCompat.START)
        }
        FirebaseAuth.getInstance().currentUser?.let {
            homeViewmodel = ViewModelProvider(this, viewModelFactory { HomeViewmodel(it) })
                .get(HomeViewmodel::class.java)
            appDetailViewmodel = ViewModelProvider(this@MainActivity).get(AppDetailViewmodel::class.java)
            videoViewmodel = ViewModelProvider(this@MainActivity).get(VideoViewmodel::class.java)
            settingViewmodel = ViewModelProvider(this@MainActivity).get(SettingViewmodel::class.java)
            locationViewmodel = ViewModelProvider(this@MainActivity).get(LocationViewmodel::class.java)
            val headerView = side_nav_view.getHeaderView(0)
            headerView.txt_email.text = it.email.toString()
            headerView.txt_name.text = it.displayName.toString()
            it.photoUrl?.let { profilePicUrl ->
                Glide.with(this@MainActivity)
                    .load(profilePicUrl)
                    .into(img_profilePic)
            }
        }
        childSelectionStateViewmodel =
            ViewModelProvider(this).get(ChildSelectionStateViewmodel::class.java)
        
        homeViewmodel.listOfChild.observe(this, isChildExistObserver)
        
        // When tab selected changes childSelectedState will change its value and notify all subscribers
        tab_child_selector.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                // tab.tag = child email
                childSelectionStateViewmodel.changeChildSelected(tab.tag.toString())
            }
            override fun onTabReselected(tab: TabLayout.Tab) {
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {
            }
        })
        // Set sign out action
        side_nav_view.menu.getItem(1).setOnMenuItemClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finishAfterTransition()
            true
        }
        swipeContainer.setOnRefreshListener {
            val currentChild = tab_child_selector.getTabAt(tab_child_selector.selectedTabPosition)?.tag
            currentChild?.let { refreshData(it.toString()) }
        }
        swipeContainer.setColorSchemeResources(R.color.colorAccent, R.color.colorGray)
    }

    override fun onStart() {
        super.onStart()
        progressbar.visibility = View.VISIBLE
        txt_belumAdaAnak.text = this.resources.getString(R.string.sedang_memuat_data)
        homeViewmodel.loadListOfChild()
    }
    
    // This observer only trigger at very first time the MainActivity was created or after add a child
    private val isChildExistObserver = Observer<MutableList<DocumentSnapshot>> { listOfChildEmail ->
        if (listOfChildEmail.size > 0) {
            tab_child_selector.removeAllTabs()
            container_content.visibility = View.VISIBLE
            for (data in listOfChildEmail) {
                val view = LayoutInflater.from(this@MainActivity).inflate(R.layout.item_child_selector, null)
                view.txt_child_name.text = data["nama"].toString()
                val tab = tab_child_selector.newTab().setCustomView(view)
                tab.tag = data["email"].toString()
                tab_child_selector.addTab(tab)
            }
            if (!tab_child_selector[0].isSelected) tab_child_selector[0].isSelected = true
        } else {
            progressbar.visibility = View.INVISIBLE
            txt_belumAdaAnak.text = this.resources.getString(R.string.belum_ada_anak)
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
    }
    
    override fun onBackPressed() {
        finish()
    }
    
    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        when (destination.id) {
            R.id.nav_home -> txt_title.text = this.resources.getString(R.string.title_home)
            R.id.nav_app_detail -> txt_title.text = this.resources.getString(R.string.title_app_detail)
            R.id.nav_video -> txt_title.text = this.resources.getString(R.string.title_video)
            R.id.nav_setting -> txt_title.text = this.resources.getString(R.string.title_setting)
            R.id.nav_location -> txt_title.text = this.resources.getString(R.string.title_location)
        }
    }

    private fun refreshData(currentChild: String) {
        when (navController.currentDestination?.id) {
            R.id.nav_home -> {
                homeViewmodel.loadChildrenUsageSum(currentChild, true)
                homeViewmodel.loadChildrenRecentApp(currentChild, true)
            }
            R.id.nav_app_detail -> appDetailViewmodel.loadDetailApps(currentChild, true)
            R.id.nav_video -> videoViewmodel.loadListOfVideos(currentChild, true)
            R.id.nav_setting -> settingViewmodel.refreshSetting(currentChild)
            R.id.nav_location -> txt_title.text = this.resources.getString(R.string.title_location)
        }
    }
}