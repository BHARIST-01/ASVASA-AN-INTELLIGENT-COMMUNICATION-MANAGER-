package com.example.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.EmergencyCenter
import com.example.data.model.EmergencyCenterType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class UserGpsLocation(
    val latitude: Double = 37.7749,
    val longitude: Double = -122.4194,
    val altitudeMeters: Double = 16.0,
    val accuracyMeters: Float = 4.5f,
    val hasFix: Boolean = true,
    val provider: String = "GPS / Fused",
    val timestamp: Long = System.currentTimeMillis()
)

class EmergencyLocatorService(private val context: Context) {

    private val tag = "EmergencyLocator"

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow(UserGpsLocation())
    val currentLocation: StateFlow<UserGpsLocation> = _currentLocation.asStateFlow()

    private val _emergencyCenters = MutableStateFlow<List<EmergencyCenter>>(emptyList())
    val emergencyCenters: StateFlow<List<EmergencyCenter>> = _emergencyCenters.asStateFlow()

    private val _isLiveTracking = MutableStateFlow(false)
    val isLiveTracking: StateFlow<Boolean> = _isLiveTracking.asStateFlow()

    private var locationCallback: LocationCallback? = null

    init {
        fetchLocation()
        recalculateDistances(_currentLocation.value.latitude, _currentLocation.value.longitude)
        startContinuousLocationUpdates()
    }

    private fun hasLocationPermission(): Boolean {
        val finePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return finePerm == PackageManager.PERMISSION_GRANTED || coarsePerm == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation() {
        if (!hasLocationPermission()) {
            Log.d(tag, "Location permission not yet granted; retaining cached fix.")
            return
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    updateUserLocation(loc)
                } else {
                    // Try lastLocation if immediate fix is null
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) updateUserLocation(lastLoc)
                    }
                }
            }.addOnFailureListener { e ->
                Log.d(tag, "FusedLocationProviderClient fallback: ${e.message}")
                fallbackToSystemLocationManager()
            }
        } catch (e: Exception) {
            Log.d(tag, "Exception fetching location via FusedLocationProviderClient: ${e.message}")
            fallbackToSystemLocationManager()
        }
    }

    @SuppressLint("MissingPermission")
    fun startContinuousLocationUpdates() {
        if (!hasLocationPermission() || _isLiveTracking.value) return

        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 8000L)
                .setMinUpdateIntervalMillis(4000L)
                .setMinUpdateDistanceMeters(2.0f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val last = result.lastLocation
                    if (last != null) {
                        updateUserLocation(last)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback as LocationCallback,
                Looper.getMainLooper()
            )
            _isLiveTracking.value = true
            Log.i(tag, "Continuous FusedLocation updates initiated.")
        } catch (e: Exception) {
            Log.d(tag, "Could not start continuous FusedLocation updates: ${e.message}")
        }
    }

    fun stopContinuousLocationUpdates() {
        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (_: Exception) {}
        }
        locationCallback = null
        _isLiveTracking.value = false
    }

    private fun updateUserLocation(loc: Location) {
        _currentLocation.value = UserGpsLocation(
            latitude = loc.latitude,
            longitude = loc.longitude,
            altitudeMeters = loc.altitude,
            accuracyMeters = loc.accuracy,
            hasFix = true,
            provider = loc.provider ?: "FusedLocationProvider",
            timestamp = System.currentTimeMillis()
        )
        recalculateDistances(loc.latitude, loc.longitude)
    }

    private fun fallbackToSystemLocationManager() {
        try {
            val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val loc = locManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                ?: locManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                updateUserLocation(loc)
            }
        } catch (_: Exception) {}
    }

    fun recalculateDistances(userLat: Double, userLon: Double) {
        val baseList = getSeedEmergencyCenters()
        val sorted = baseList.map { center ->
            val dist = calculateDistanceKm(userLat, userLon, center.latitude, center.longitude)
            val roundedDist = (dist * 10).roundToInt() / 10.0
            val estMinutes = (roundedDist * 4.5).roundToInt().coerceAtLeast(3)
            center.copy(distanceKm = roundedDist, estimatedResponseTimeMinutes = estMinutes)
        }.sortedBy { it.distanceKm }

        _emergencyCenters.value = sorted
    }

    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    private fun getSeedEmergencyCenters(): List<EmergencyCenter> {
        return listOf(
            EmergencyCenter(
                id = "pol_01",
                name = "Central Metro Police Precinct",
                type = EmergencyCenterType.POLICE,
                latitude = 37.7790,
                longitude = -122.4150,
                distanceKm = 0.8,
                phoneNumber = "100",
                address = "450 7th St, Emergency Command Division",
                open24Hours = true,
                specialCapabilities = "Tactical response, incident triage & dispatch"
            ),
            EmergencyCenter(
                id = "fire_01",
                name = "Station 12 Fire & Rescue Hub",
                type = EmergencyCenterType.FIRE,
                latitude = 37.7830,
                longitude = -122.4280,
                distanceKm = 1.4,
                phoneNumber = "101",
                address = "1145 Market St, Hazard Response Unit",
                open24Hours = true,
                specialCapabilities = "Heavy search & rescue, structural extraction"
            ),
            EmergencyCenter(
                id = "hosp_01",
                name = "Apex General Trauma & ICU",
                type = EmergencyCenterType.HOSPITAL,
                latitude = 37.7690,
                longitude = -122.4110,
                distanceKm = 2.1,
                phoneNumber = "102",
                address = "1001 Potrero Ave, Emergency Care Level 1",
                open24Hours = true,
                specialCapabilities = "Level-1 Trauma, Burn unit, Surgical ICU"
            ),
            EmergencyCenter(
                id = "relief_01",
                name = "Civic Disaster Relief & Red Cross Shelter",
                type = EmergencyCenterType.DISASTER_RELIEF,
                latitude = 37.7815,
                longitude = -122.4040,
                distanceKm = 3.2,
                phoneNumber = "1070",
                address = "747 Howard St, Emergency Supply Depot",
                open24Hours = true,
                specialCapabilities = "Emergency food, clean water, satellite satellite uplink"
            ),
            EmergencyCenter(
                id = "pol_02",
                name = "North Harbor Police Station",
                type = EmergencyCenterType.POLICE,
                latitude = 37.7950,
                longitude = -122.4200,
                distanceKm = 3.9,
                phoneNumber = "100",
                address = "601 Beach St, Maritime & Coastal Security",
                open24Hours = true,
                specialCapabilities = "Rapid mobile patrol & evacuation escort"
            )
        )
    }

    fun openMapsNavigation(center: EmergencyCenter) {
        try {
            val uri = Uri.parse("geo:${center.latitude},${center.longitude}?q=${center.latitude},${center.longitude}(${Uri.encode(center.name)})")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun composeSosSms(emergencyContacts: String, bloodType: String, coordinates: UserGpsLocation): Intent {
        val body = "EMERGENCY SOS: I need immediate assistance! Location: Lat ${coordinates.latitude}, Lon ${coordinates.longitude} (Accuracy: ${coordinates.accuracyMeters}m). Blood Group: $bloodType. Sent via ASVASA Emergency Mesh."
        return Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:$emergencyContacts")
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
