package com.gryffindor.smartshopping.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.gryffindor.smartshopping.app.navigation.AppNavGraph
import com.gryffindor.smartshopping.data.meta.WearablesInitializer

class MainActivity : ComponentActivity() {

    private var wearablesSetupDone = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.entries.all { it.value }
        if (allGranted) {
            setupWearables()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as SmartShoppingApp).appContainer

        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    AppNavGraph(
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
