package com.dodolz.kiddos.navigation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.afollestad.materialdialogs.MaterialDialog
import com.dodolz.kiddos.R
import com.dodolz.kiddos.viewmodel.ChildSelectionStateViewmodel
import com.dodolz.kiddos.viewmodel.LocationViewmodel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.markerview.MarkerView
import com.mapbox.mapboxsdk.plugins.markerview.MarkerViewManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat", "SetTextI18n")
class LocationFragment : Fragment() {
    
    private var markerViewManager: MarkerViewManager? = null
    private val childSelectionStateViewmodel: ChildSelectionStateViewmodel by activityViewModels()
    private val viewmodel: LocationViewmodel by activityViewModels()
    private lateinit var loadingDialog: MaterialDialog
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireContext().also { Mapbox.getInstance(it, getString(R.string.mapbox_access_token)) }
    }
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map_View?.onCreate(savedInstanceState)
        loadingDialog = MaterialDialog(requireContext())
            .title(text = "Memuat Data...")
            .message(text = "Mohon tunggu")
            .icon(R.drawable.ic_loading)
        childSelectionStateViewmodel.childSelected.observe(viewLifecycleOwner, Observer { childEmail ->
            loadingDialog.show()
            loadMaps(childEmail)
        })
        viewmodel.isUserRefreshing.observe(viewLifecycleOwner, Observer {
            if (it.first) loadMaps(it.second)
        })
    }

    private fun loadMaps(childEmail: String) {
        Firebase.firestore.collection("User").document(childEmail).collection("Lokasi")
            .document("lokasi").get()
            .addOnSuccessListener {
                if (it["lat"] != null && it["long"] != null && it["waktuDimutakhirkan"] != null
                    && it["lat"].toString().isNotBlank() && it["long"].toString().isNotBlank()
                    && it["waktuDimutakhirkan"].toString().isNotBlank()
                ) {
                    val lat: Double = it["lat"].toString().toDouble()
                    val long: Double = it["long"].toString().toDouble()
                    val timestamp: Long = it["waktuDimutakhirkan"].toString().toLong()
                    val sdf = SimpleDateFormat("HH:mm")
                    val netDate = Date(timestamp)
                    txt_waktuUpdate.text = "Dimutakhirkan ${sdf.format(netDate)}"
                    map_View?.getMapAsync { mapboxMap ->
                        mapboxMap.setStyle(Style.OUTDOORS)
                        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, long), 14.0))
                        val markerView = ImageView(requireContext())
                        markerView.layoutParams = LinearLayout.LayoutParams(100, 100)
                        markerView.setImageResource(R.drawable.ic_location_24)
                        markerViewManager = MarkerViewManager(map_View, mapboxMap)
                        val marker = MarkerView(LatLng(lat, long), markerView)
                        markerViewManager?.addMarker(marker)
                        val reverseGeocode = MapboxGeocoding.builder()
                            .accessToken(getString(R.string.mapbox_access_token))
                            .query(Point.fromLngLat(long, lat))
                            .geocodingTypes(GeocodingCriteria.TYPE_POI)
                            .build()
                        reverseGeocode.enqueueCall(object : Callback<GeocodingResponse> {
                            @SuppressLint("SetTextI18n")
                            override fun onResponse(
                                call: Call<GeocodingResponse>,
                                response: Response<GeocodingResponse>
                            ) {
                                val results = response.body()!!.features()
                                if (results.size > 0) {
                                    // Log the first results Point.
                                    val firstResultPoint = results[0].placeName()
                                    if (firstResultPoint != null)
                                        txt_alamatLokasi.text = "Alamat Lokasi: $firstResultPoint"
                                    else
                                        txt_alamatLokasi.text = "Alamat Lokasi: Belum tersedia"
                                } else {
                                    txt_alamatLokasi.text = "Alamat Lokasi: Belum tersedia"
                                }
                                loadingDialog.dismiss()
                            }
                            override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
                                throwable.printStackTrace()
                                loadingDialog.dismiss()
                            }
                        })
                    }
                    cardView4.visibility = View.VISIBLE
                } else {
                    cardView4.visibility = View.INVISIBLE
                    loadingDialog.dismiss()
                }
                requireActivity().swipeContainer.isRefreshing = false
            }
            .addOnFailureListener {
                cardView4.visibility = View.INVISIBLE
                loadingDialog.dismiss()
                requireActivity().swipeContainer.isRefreshing = false
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        markerViewManager?.onDestroy()
        map_View.onDestroy()
    }
}