package com.example.speedometer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.speedometer.ui.theme.SpeedoMeterTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClientProvider : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequired : Boolean = false


    private val permissions = arrayOf(
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
    @SuppressLint("UnrememberedMutableState")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fusedLocationClientProvider = LocationServices.getFusedLocationProviderClient(this)


        setContent {
            val context = LocalContext.current

            var currentLocation by remember {
                mutableStateOf(LatLng(0.0,0.0))
            }

            var speedKmph by remember {
                mutableStateOf(0f)
            }

            val animateSpeedKmph : Float by animateFloatAsState(
                targetValue = if (speedKmph>260f) 300f else speedKmph,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "SpeedKmpH", )

            val cameraPosition = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(
                    currentLocation, 10f
                )
            }

            var cameraPositionState by remember {
                mutableStateOf(cameraPosition)
            }


            locationCallback = object : LocationCallback() {
                private var previousLocation: LatLng? = null
                private var previousTime: Long? = null

                override fun onLocationResult(p0: LocationResult) {
                    Log.d("GET LOCATION SPEED", "current Location = $currentLocation ")

                    super.onLocationResult(p0)
                    for (location in p0.locations) {
                        currentLocation = LatLng(location.latitude, location.longitude)
                        val currentTime = location.time

                        if (previousLocation != null && previousTime != null) {
                            val distance = FloatArray(1)

                            Location.distanceBetween(
                                previousLocation!!.latitude, previousLocation!!.longitude,
                                currentLocation.latitude, currentLocation.longitude,
                                distance
                            )

                            val timeDifference = (currentTime - previousTime!!)/1000.0

                            speedKmph = ((distance[0] / timeDifference) * 3.6).toFloat() + 60f

                            Log.d("SPEED METER", "Speed : $speedKmph km/h")
                        }

                        previousLocation = currentLocation
                        previousTime = currentTime

                        Log.d("DISTANCE", "current Location = $currentLocation ")
//                        cameraPositionState = CameraPositionState(
//                            position = CameraPosition.fromLatLngZoom(
//                                currentLocation, 10f
//                            )
//                        )


                    }
                }
            }
            val launchMultiplePermissions = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions() ) { permissions ->
                val areGranted = permissions.values.reduce { acc, next -> acc && next}
                if (areGranted) {
                    locationRequired = true
                    startLocationUpdates()
                    Toast.makeText(context, "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    locationRequired = false
                    Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }

            if (permissions.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }) {
                val isLocationEnabled = isLocationServiceEnabled(context)

                if (isLocationEnabled) {
                    startLocationUpdates()
                    Toast.makeText(context, "Permissions Granted & Location is Enabled", Toast.LENGTH_SHORT).show()

                } else {
                    // Prompt user to enable location services
                    Toast.makeText(context, "Please enable location services", Toast.LENGTH_SHORT).show()
                    promptEnableLocation(context)
                }
                //get Location
            } else {
//                launchMultiplePermissions.launch(permissions)
                PermissionHandler(permissions = permissions, startUpdateLocation = ::startLocationUpdates)
            }


//            locationRequired = PermissionHandler(permissions = permissions, locationCallback!!, fusedLocationClientProvider)
//
//            if (locationRequired) {
//                Log.d("CHECK PERMISSION", "ISLOCATIONENABLED : Permission is granted ")
//            } else {
//                Log.d("CHECK PERMISSION", "ISLOCATIONENABLED : Permission is needed ")
//            }

//            GoogleMap(
//                modifier = Modifier.fillMaxSize(),
//                cameraPositionState = cameraPositionState
//            ) {
//                Marker(
//                    state = MarkerState(position = currentLocation)
//                )
//
//            }
            SpeedometerScreen(modifier = Modifier,currentLocation, animateSpeedKmph)
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationRequired){
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        if (!locationRequired) {
            locationCallback.let {
                fusedLocationClientProvider.removeLocationUpdates(it)
            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        Log.d("GET LOCATION", "Start location")
        locationCallback?.let {
//            val locationRequest = LocationRequest.create().apply {
//                interval = 1000
//                fastestInterval = 5000
//                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//            }
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 1000
            )
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(3000)
                .setMaxUpdateDelayMillis(100)
                .build()

            fusedLocationClientProvider.requestLocationUpdates(
                locationRequest, it, Looper.getMainLooper()
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun startLocationUpdates(locationCallback: LocationCallback, fusedLocationClientProvider: FusedLocationProviderClient) {
    Log.d("GET LOCATION", "Start location")
    locationCallback?.let {
//            val locationRequest = LocationRequest.create().apply {
//                interval = 1000
//                fastestInterval = 5000
//                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//            }
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1000
        )
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .setMaxUpdateDelayMillis(100)
            .build()

        fusedLocationClientProvider.requestLocationUpdates(
            locationRequest, it, Looper.getMainLooper()
        )
    }
}


@Composable
fun PermissionHandler(permissions: Array<String>, startUpdateLocation: () -> Unit ) {
    val context = LocalContext.current
    // State to track whether permissions are granted
    var arePermissionsGranted by remember { mutableStateOf(false) }
    var isLocationEnabled by remember { mutableStateOf(false) }

    // Launcher for requesting multiple permissions
    val launcherMultiplePermissions = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
//        val areGranted = permissionsMap.values.reduce { acc, next -> acc && next }
//
//        if (areGranted) {
//            Toast.makeText(context, "Permission Granted", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
//        }

        arePermissionsGranted = permissionsMap.values.all { it }

        if (arePermissionsGranted) {
            isLocationEnabled = isLocationServiceEnabled(context)

            if (isLocationEnabled) {
                startUpdateLocation()
                Toast.makeText(context, "Permissions Granted & Location is Enabled", Toast.LENGTH_SHORT).show()

            } else {
                // Prompt user to enable location services
                Toast.makeText(context, "Please enable location services", Toast.LENGTH_SHORT).show()
                promptEnableLocation(context)
            }
        } else {
            Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Check if permissions are already granted
    LaunchedEffect(Unit) {
        if (permissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }) {
            arePermissionsGranted = true

            isLocationEnabled = isLocationServiceEnabled(context)

            if (!isLocationEnabled) {
                promptEnableLocation(context)
                Log.d("TAG","prompt is called")
            }
            Toast.makeText(context, "All permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            // Request permissions
            launcherMultiplePermissions.launch(permissions)
        }
    }
//
//    return isLocationEnabled
}

//function to check if location services are enabled
fun isLocationServiceEnabled(context: Context) : Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

fun promptEnableLocation(context: Context) {
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    context.startActivity(intent)
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SpeedoMeterTheme {
        Greeting("Android")
    }
}