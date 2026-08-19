package com.gryffindor.smartshopping.app

import android.Manifest
import android.content.pm.PackageManager
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
        val allGranted = results.entries.all { it.value }
        if (allGranted) {
            setupWearables()
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

        // Request BLUETOOTH_CONNECT once during Activity creation.
        // Only proceed with Wearables setup after permission is granted.
        if (isBluetoothConnectGranted()) {
            setupWearables()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT))
        }
    }

    private fun isBluetoothConnectGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.BLUETOOTH_CONNECT
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
