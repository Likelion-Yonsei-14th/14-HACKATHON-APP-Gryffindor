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

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.INTERNET
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.entries.all { it.value }
        if (allGranted) {
            initializeWearables()
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
    }

    override fun onStart() {
        super.onStart()
        // Check and request permissions, then initialize DAT SDK.
        if (hasRequiredPermissions()) {
            initializeWearables()
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun initializeWearables() {
        // Initialize DAT SDK (idempotent — safe to call multiple times)
        WearablesInitializer.initialize(this)
        // Start registration to enable device discovery
        WearablesInitializer.startRegistration(this)
    }
}
