package com.gryffindor.smartshopping.core.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Simple lat/lng result from a one-shot location fetch.
 * Not stored anywhere — used transiently for Feed API calls.
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double
)

/**
 * Provides one-shot current location using FusedLocationProviderClient.
 * Returns null gracefully if permission is denied or location unavailable.
 */
class LocationProvider(private val context: Context) {

    companion object {
        private const val TAG = "LocationProvider"
    }

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Returns true if location permission is granted (fine or coarse).
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * One-shot location fetch. Returns null if:
     * - Permission not granted
     * - Location unavailable
     * - Timeout / error
     *
     * Does NOT store the location anywhere.
     */
    suspend fun getCurrentLocation(): LatLng? {
        if (!hasPermission()) {
            Log.d(TAG, "Location permission not granted, returning null")
            return null
        }

        return try {
            getLocationInternal()
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException getting location", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get location", e)
            null
        }
    }

    @SuppressWarnings("MissingPermission")
    private suspend fun getLocationInternal(): LatLng? {
        return suspendCancellableCoroutine { continuation ->
            val cancellationToken = CancellationTokenSource()

            continuation.invokeOnCancellation {
                cancellationToken.cancel()
            }

            fusedClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationToken.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Location obtained: ${location.latitude}, ${location.longitude}")
                    continuation.resume(LatLng(location.latitude, location.longitude))
                } else {
                    Log.d(TAG, "Location result was null")
                    continuation.resume(null)
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "Location fetch failed", e)
                continuation.resume(null)
            }
        }
    }
}
