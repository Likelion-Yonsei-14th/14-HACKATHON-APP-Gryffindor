package com.gryffindor.smartshopping.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.gryffindor.smartshopping.app.navigation.ProductionNavGraph
import com.gryffindor.smartshopping.core.ui.theme.SmartShoppingTheme
import com.gryffindor.smartshopping.data.meta.WearablePermissionRequester
import com.gryffindor.smartshopping.data.meta.WearableUpdateRequester
import com.gryffindor.smartshopping.data.meta.WearablesInitializer
import com.gryffindor.smartshopping.domain.camera.GlassesUpdateResult
import com.meta.wearable.dat.core.Wearables
import com.meta.wearable.dat.core.types.Permission
import com.meta.wearable.dat.core.types.PermissionStatus
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var wearablesSetupDone = false

    // --- Android Bluetooth permission ---

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Wearables SDK requires BLUETOOTH_CONNECT; POST_NOTIFICATIONS is optional.
        val bluetoothGranted = results[Manifest.permission.BLUETOOTH_CONNECT] ?: true
        if (bluetoothGranted) {
            setupWearables()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = results[Manifest.permission.POST_NOTIFICATIONS] ?: true
            Log.i(TAG, "POST_NOTIFICATIONS granted=$notifGranted")
        }
    }

    // --- DAT Wearable Camera Permission ---

    private var permissionContinuation: CancellableContinuation<Boolean>? = null
    private val permissionMutex = Mutex()

    private val wearablePermissionLauncher = registerForActivityResult(
        Wearables.RequestPermissionContract()
    ) { result ->
        val granted = result.getOrNull() is PermissionStatus.Granted
        Log.i(TAG, "Wearable camera permission result: granted=$granted")
        permissionContinuation?.resume(granted)
        permissionContinuation = null
    }

    /**
     * Implementation of [WearablePermissionRequester] that uses the Activity's
     * registered ActivityResultLauncher to request DAT camera permission.
     */
    val wearablePermissionRequester: WearablePermissionRequester = object : WearablePermissionRequester {
        override suspend fun requestCameraPermission(): Boolean {
            return permissionMutex.withLock {
                suspendCancellableCoroutine { continuation ->
                    permissionContinuation = continuation
                    continuation.invokeOnCancellation { permissionContinuation = null }
                    Log.i(TAG, "Launching DAT camera permission request")
                    wearablePermissionLauncher.launch(Permission.CAMERA)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as SmartShoppingApp).appContainer

        // Wire the permission requester to MetaCameraSource
        appContainer.metaCameraSource.permissionRequester = wearablePermissionRequester

        // Wire the update requester to MetaCameraSource
        appContainer.metaCameraSource.updateRequester = WearableUpdateRequester {
            val result = Wearables.openDATGlassesAppUpdate(this@MainActivity)
            when {
                result.isSuccess -> GlassesUpdateResult.Success
                else -> {
                    val error = result.errorOrNull()
                    GlassesUpdateResult.Failed(error?.description ?: "Unknown error")
                }
            }
        }

        setContent {
            SmartShoppingTheme {
                Surface {
                    val navController = rememberNavController()
                    ProductionNavGraph(
                        navController = navController,
                        appContainer = appContainer
                    )
                }
            }
        }

        // Request runtime permissions once during Activity creation.
        // BLUETOOTH_CONNECT is required for Wearables SDK; POST_NOTIFICATIONS for Android 13+.
        val permissionsToRequest = buildList {
            if (!isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isEmpty()) {
            setupWearables()
        } else {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this, permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Initialize and register Wearables SDK exactly once.
     * Called only after BLUETOOTH_CONNECT is confirmed granted.
     */
    private fun setupWearables() {
        if (wearablesSetupDone) return
        wearablesSetupDone = true

        // 1. Initialize (requires BLUETOOTH_CONNECT granted)
        WearablesInitializer.initialize(this)

        // 2. Register only if not already REGISTERED
        WearablesInitializer.startRegistrationIfNeeded(this)
    }
}
