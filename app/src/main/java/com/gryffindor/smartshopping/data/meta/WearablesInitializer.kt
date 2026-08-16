package com.gryffindor.smartshopping.data.meta

import android.app.Activity
import android.content.Context
import android.util.Log
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.RegistrationState
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles Meta DAT SDK initialization and registration.
 *
 * Lifecycle (per Meta guidance):
 * 1. BLUETOOTH_CONNECT must be GRANTED before calling [initialize].
 * 2. [initialize] is called exactly once per app lifecycle.
 * 3. [startRegistrationIfNeeded] checks registrationState and only calls
 *    Wearables.startRegistration() if the SDK is NOT already REGISTERED.
 * 4. No repeated startRegistration on every onStart/onResume.
 *
 * All Meta SDK types remain inside data/meta/ boundary.
 */
object WearablesInitializer {

    private const val TAG = "WearablesInitializer"

    private val initialized = AtomicBoolean(false)
    private val registrationRequested = AtomicBoolean(false)

    /**
     * Initialize the DAT SDK. Must be called after BLUETOOTH_CONNECT is granted.
     * Only the first call takes effect.
     */
    fun initialize(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            try {
                Wearables.initialize(context.applicationContext)
                Log.i(TAG, "Wearables SDK initialized successfully")
            } catch (e: Exception) {
                initialized.set(false)
                Log.e(TAG, "Wearables.initialize() failed", e)
            }
        }
    }

    /**
     * Start DAT registration only if not already REGISTERED.
     * Must be called after [initialize] and from an Activity context.
     * Only the first successful request takes effect.
     */
    fun startRegistrationIfNeeded(activity: Activity) {
        if (!initialized.get()) {
            Log.w(TAG, "Cannot register — SDK not initialized")
            return
        }

        // Check current registration state before requesting
        val currentState = try {
            Wearables.registrationState.value
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read registrationState", e)
            null
        }

        Log.i(TAG, "registrationState=$currentState")

        if (currentState == RegistrationState.REGISTERED) {
            Log.i(TAG, "Already REGISTERED — skipping startRegistration()")
            logDeviceDiagnostics()
            return
        }

        if (registrationRequested.compareAndSet(false, true)) {
            try {
                Wearables.startRegistration(activity)
                Log.i(TAG, "Wearables.startRegistration() called (state was: $currentState)")
            } catch (e: Exception) {
                registrationRequested.set(false)
                Log.e(TAG, "Wearables.startRegistration() failed", e)
            }
        } else {
            Log.d(TAG, "Registration already requested — skipping duplicate call")
        }
    }

    /**
     * Log diagnostic information about the current DAT state.
     * Called before camera start and after registration.
     */
    fun logDiagnostics() {
        if (!initialized.get()) {
            Log.w(TAG, "[diag] SDK not initialized")
            return
        }

        try {
            val regState = Wearables.registrationState.value
            Log.i(TAG, "[diag] registrationState=$regState")
        } catch (e: Exception) {
            Log.w(TAG, "[diag] Failed to read registrationState", e)
        }

        logDeviceDiagnostics()
        logCameraPermissionStatus()
    }

    private fun logDeviceDiagnostics() {
        try {
            val devices = Wearables.devices.value
            Log.i(TAG, "[diag] devices.count=${devices.size}")
            devices.forEachIndexed { index, deviceId ->
                Log.i(TAG, "[diag] device[$index]=$deviceId")
                try {
                    val metadata = Wearables.devicesMetadata[deviceId]
                    if (metadata != null) {
                        // devicesMetadata is a StateFlow map — read current value
                        val meta = metadata.value
                        Log.i(TAG, "[diag] device[$index].name=${meta.name}, compatibility=${meta.compatibility}")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "[diag] Could not read metadata for device[$index]", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "[diag] Failed to read devices", e)
        }
    }

    private fun logCameraPermissionStatus() {
        // Wearables.checkPermissionStatus is suspend — cannot call from sync context.
        // Camera permission status will be observable when stream actually starts/fails.
        Log.d(TAG, "[diag] cameraPermission: verify via startCamera attempt")
    }

    /** Whether the SDK has been successfully initialized. */
    val isInitialized: Boolean get() = initialized.get()
}
