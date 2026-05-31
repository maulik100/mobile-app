package com.chehartemple.app.data.api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    private var cachedLocation: String? = null

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getLocationString(context: Context): String? {
        if (!hasPermission(context)) return null
        cachedLocation?.let { return it }

        val location = getCurrentLocation(context) ?: return null
        val address = getAddress(context, location)
        cachedLocation = address ?: "${location.latitude},${location.longitude}"
        return cachedLocation
    }

    @Suppress("MissingPermission")
    private suspend fun getCurrentLocation(context: Context): Location? {
        return suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc -> cont.resume(loc) }
                .addOnFailureListener { cont.resume(null) }
            cont.invokeOnCancellation { cts.cancel() }
        }
    }

    private fun getAddress(context: Context, location: Location): String? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            addresses?.firstOrNull()?.let { addr ->
                listOfNotNull(addr.locality, addr.adminArea, addr.countryName)
                    .joinToString(", ")
                    .ifEmpty { null }
            }
        } catch (_: Exception) { null }
    }

    fun clearCache() { cachedLocation = null }
}
