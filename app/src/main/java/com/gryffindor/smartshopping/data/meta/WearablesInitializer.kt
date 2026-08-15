package com.gryffindor.smartshopping.data.meta

import android.app.Activity
import android.content.Context
import android.util.Log
import com.meta.wearable.dat.core.Wearables
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Handles Meta DAT SDK initialization and registration.
 *
 * The DAT SDK requires:
 * 1. Wearables.initialize(context) — once per app lifecycle
 * 2. Wearables.startRegistration(activity) — to discover/connect to wearable devices
 *
 * Both must complete before AutoDeviceSelector / createSession can succeed.
 *
 * All Meta SDK types remain inside data/meta/ boundary.
 */
object WearablesInitializer {

    private const val TAG = "WearablesInitializer"

    private val initialized = AtomicBoolean(false)
    private val registered = AtomicBoolean(false)

    /**
     * Initialize the DAT SDK. Call once from Application.onCreate() or early Activity lifecycle.
     * Safe to call multiple times — only the first call takes effect.
     */
    fun initialize(context: Context) {
        if (initialized.compareAndSet(false, true)) {
            try {
                Wearables.initialize(context.applicationContext)
                Log.i(TAG, "Wearables SDK initialized")
            } catch (e: Exception) {
                initialized.set(false)
                Log.e(TAG, "Wearables.initialize failed", e)
            }
        }
    }

    /**
     * Start DAT registration to enable device discovery.
     * Must be called after [initialize] and from an Activity context.
     * Safe to call multiple times — only the first call takes effect.
     */
    fun startRegistration(activity: Activity) {
        if (!initialized.get()) {
            Log.w(TAG, "Cannot register — SDK not initialized")
            return
        }
        if (registered.compareAndSet(false, true)) {
            try {
                Wearables.startRegistration(activity)
                Log.i(TAG, "Wearables registration started")
            } catch (e: Exception) {
                registered.set(false)
                Log.e(TAG, "Wearables.startRegistration failed", e)
            }
        }
    }

    /** Whether the SDK has been successfully initialized. */
    val isInitialized: Boolean get() = initialized.get()
}
