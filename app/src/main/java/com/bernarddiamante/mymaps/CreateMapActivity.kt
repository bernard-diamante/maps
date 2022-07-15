package com.bernarddiamante.mymaps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentProviderClient
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bernarddiamante.mymaps.MainActivity.Companion.EXTRA_MAP_TITLE
import com.bernarddiamante.mymaps.MainActivity.Companion.EXTRA_USER_MAP

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.bernarddiamante.mymaps.databinding.ActivityCreateMapBinding
import com.bernarddiamante.mymaps.models.Place
import com.bernarddiamante.mymaps.models.UserMap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.Marker
import com.google.android.material.snackbar.Snackbar
import java.util.*

class CreateMapActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "CreateMapActivity"
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityCreateMapBinding

    private var markers: MutableList<Marker> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCreateMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = intent.getStringExtra(EXTRA_MAP_TITLE)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mapFragment.view?.let { makeSnackbar(it, "Long press to add a marker!") }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_map, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Saving map
        if (item.itemId == R.id.miSave) {
            Log.i(TAG, "Tapped on save")
            // Check if map is valid (has markers) then save map
            if (markers.isEmpty()) {
                Toast.makeText(this, "Map must have at least one marker", Toast.LENGTH_SHORT).show()
                return true
            }

            val title = intent.getStringExtra(EXTRA_MAP_TITLE)
            val places = markers.map { marker ->
                Place(
                    marker.title,
                    marker.snippet,
                    marker.position.latitude,
                    marker.position.longitude
                )
            }
            val userMap = title?.let { UserMap(it, places) }
            val data = Intent()
            data.putExtra(EXTRA_USER_MAP, userMap)
            setResult(Activity.RESULT_OK, data)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.setOnInfoWindowLongClickListener {
            Log.i(TAG, "onWindowLongClickListener - Delete this marker")
            markers.remove(it)
            it.remove()
        }
        mMap.setOnMapLongClickListener { latLng ->
            Log.i(TAG, "onMapLongClickListener")
            showAlertDialog(latLng)
        }

        // Move camera to current location
        val lat = intent.getDoubleExtra("latitude", 0.0)
        val long = intent.getDoubleExtra("longitude", 0.0)
        val camLocation = LatLng(lat, long)

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(camLocation, 18f))
    }

    private fun showAlertDialog(latLng: LatLng) {
        val placeFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_place, null)
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Create a marker")
                .setView(placeFormView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", null)
                .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val title = placeFormView.findViewById<EditText>(R.id.etTitle).text.toString()
            val description =
                placeFormView.findViewById<EditText>(R.id.etDescription).text.toString()
            if (title.trim().isEmpty() || description.trim().isEmpty()) {
                Toast.makeText(
                    this,
                    "Place must have a title and a description",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .snippet(description)
            )
            markers.add(marker)
            dialog.dismiss()
        }
    }

    fun makeSnackbar(view: View, string: String) {
        Snackbar.make(view, string, Snackbar.LENGTH_INDEFINITE)
            .setAction("OK", {})
            .setActionTextColor(ContextCompat.getColor(this, android.R.color.white))
            .show()
    }

}