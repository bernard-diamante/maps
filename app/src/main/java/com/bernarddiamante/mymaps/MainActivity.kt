package com.bernarddiamante.mymaps

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bernarddiamante.mymaps.databinding.ActivityMainBinding
import com.bernarddiamante.mymaps.models.Place
import com.bernarddiamante.mymaps.models.UserMap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.io.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_USER_MAP = "EXTRA_USER_MAP"
        const val EXTRA_MAP_TITLE = "EXTRA_MAP_TITLE"
        const val FILENAME = "usermaps.data."
        private const val permissionId = 2
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var userMaps: MutableList<UserMap>
    private lateinit var mapAdapter: MapsAdapter
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private var lat by Delegates.notNull<Double>()
//    private var long by Delegates.notNull<Double>()
    private lateinit var title: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        userMaps = deserializeUserMaps(this).toMutableList()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // If the launched activity finishes, take the data passed in then save new object.
        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val userMap = it.data?.getSerializableExtra(EXTRA_USER_MAP) as UserMap
                    Log.i(TAG, "registerForActivityResult with new map title: ${userMap.title}")
                    userMaps.add(userMap)
                    mapAdapter.notifyItemInserted(userMaps.size - 1)
                    serializeUserMaps(this, userMaps)
                }
            }
        binding.rvMaps.layoutManager = LinearLayoutManager(this)
        mapAdapter = MapsAdapter(this, userMaps, object : MapsAdapter.RecyclerViewInterface {
            override fun onItemClick(position: Int) {
                Log.i(TAG, "onItemClick $position")

                // When user taps on view in RV, navigate to DisplayMapActivity
                val intent = Intent(this@MainActivity, DisplayMapActivity::class.java)
                intent.putExtra(EXTRA_USER_MAP, userMaps[position])
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

            override fun onItemLongClick(position: Int) {
                Log.i(TAG, "onItemLongClick $position")

                // When user long presses on view, delete item
                userMaps.remove(userMaps[position])
                mapAdapter.notifyItemRemoved(position)
                serializeUserMaps(this@MainActivity, userMaps)

            }
        })
        binding.rvMaps.adapter = mapAdapter

        binding.fabCreateMap.setOnClickListener {
            Log.i(TAG, "Tap on FAB")
            showAlertDialog()
        }
    }

    private fun showAlertDialog() {
        val mapFormView = LayoutInflater.from(this).inflate(R.layout.dialog_create_map, null)
        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Map title")
                .setView(mapFormView)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", null)
                .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            title = mapFormView.findViewById<EditText>(R.id.etTitle).text.toString()
            Log.i(TAG, title)

            if (title.trim().isEmpty()) {
                Toast.makeText(this, "Map must have a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Navigate to create map activity
            getLocation()
            dialog.dismiss()
        }
    }

    // Check if user has already given permission to access location
    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(TAG, "Permissions checked - meron")
            return true
        }
        Log.i(TAG, "Permissions checked - wala")
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
        Log.i(TAG, "getLocation requestPermissions")
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }


    /*
    * 1. Check if user has already given permission to access location.
    *   else request for permission.
    * 2. Check if device GPS and network providers are enabled.
    *   else proceed with intent anyway and center map at hardcoded location
    * 3. Assign values to lat and long then launch CreateMapActivity
    * */
    @SuppressLint("MissingPermission", "SetTextI18n")
    private fun getLocation() {
        Log.i(TAG, "getLocation is called")
        if (checkPermissions()) {
            if (isLocationEnabled()) {
                fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    Log.i(TAG, "Check if task is null: ${task.result}")
                    val result = task.result
                    Log.i(TAG, "getLocation() - addOnCompleteListener()")
                    if (result != null) {
                        val currentLocation = LatLng(result.latitude, result.longitude)
                        val intent = Intent(this@MainActivity, CreateMapActivity::class.java)
                        intent.putExtra(EXTRA_MAP_TITLE, title)
                        intent.putExtra("currentLocation", currentLocation)
                        resultLauncher.launch(intent)
                    }
                }
            } else {
                Toast.makeText(this, "Please turn on location in settings", Toast.LENGTH_LONG)
                    .show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }


    private fun generateSampleData(): List<UserMap> {
        return listOf(
            UserMap(
                "Memories from University",
                listOf(
                    Place("Branner Hall", "Best dorm at Stanford", 37.426, -122.163),
                    Place(
                        "Gates CS building",
                        "Many long nights in this basement",
                        37.430,
                        -122.173
                    ),
                    Place("Pinkberry", "First date with my wife", 37.444, -122.170)
                )
            ),
            UserMap(
                "January vacation planning!",
                listOf(
                    Place("Tokyo", "Overnight layover", 35.67, 139.65),
                    Place("Ranchi", "Family visit + wedding!", 23.34, 85.31),
                    Place("Singapore", "Inspired by \"Crazy Rich Asians\"", 1.35, 103.82)
                )
            ),
            UserMap(
                "Singapore travel itinerary",
                listOf(
                    Place("Gardens by the Bay", "Amazing urban nature park", 1.282, 103.864),
                    Place(
                        "Jurong Bird Park",
                        "Family-friendly park with many varieties of birds",
                        1.319,
                        103.706
                    ),
                    Place("Sentosa", "Island resort with panoramic views", 1.249, 103.830),
                    Place(
                        "Botanic Gardens",
                        "One of the world's greatest tropical gardens",
                        1.3138,
                        103.8159
                    )
                )
            ),
            UserMap(
                "My favorite places in the Midwest",
                listOf(
                    Place(
                        "Chicago",
                        "Urban center of the midwest, the \"Windy City\"",
                        41.878,
                        -87.630
                    ),
                    Place(
                        "Rochester, Michigan",
                        "The best of Detroit suburbia",
                        42.681,
                        -83.134
                    ),
                    Place(
                        "Mackinaw City",
                        "The entrance into the Upper Peninsula",
                        45.777,
                        -84.727
                    ),
                    Place("Michigan State University", "Home to the Spartans", 42.701, -84.482),
                    Place("University of Michigan", "Home to the Wolverines", 42.278, -83.738)
                )
            ),
            UserMap(
                "Restaurants to try",
                listOf(
                    Place("Champ's Diner", "Retro diner in Brooklyn", 40.709, -73.941),
                    Place(
                        "Althea",
                        "Chicago upscale dining with an amazing view",
                        41.895,
                        -87.625
                    ),
                    Place("Shizen", "Elegant sushi in San Francisco", 37.768, -122.422),
                    Place(
                        "Citizen Eatery",
                        "Bright cafe in Austin with a pink rabbit",
                        30.322,
                        -97.739
                    ),
                    Place(
                        "Kati Thai",
                        "Authentic Portland Thai food, served with love",
                        45.505,
                        -122.635
                    )
                )
            )
        )
    }

    // Get file
    private fun getDataFile(context: Context): File {
        Log.i(TAG, "Getting file from directory ${context.filesDir}")
        return File(context.filesDir, FILENAME)
    }

    // Write into file
    private fun serializeUserMaps(context: Context, userMaps: List<UserMap>) {
        Log.i(TAG, "serializeUserMaps")
        ObjectOutputStream(FileOutputStream(getDataFile(context))).use {
            it.writeObject(userMaps)
        }
    }

    // Read from file
    private fun deserializeUserMaps(context: Context): List<UserMap> {
        Log.i(TAG, "deserializeUserMaps")
        val dataFile = getDataFile(context)
        if (!dataFile.exists()) {
            Log.i(TAG, "deserializeUserMaps")
            return emptyList()
        }
        ObjectInputStream(FileInputStream(dataFile)).use { return it.readObject() as List<UserMap> }
    }
}