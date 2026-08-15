package com.gryffindor.smartshopping.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as SmartShoppingApp).appContainer

        setContent {
            MaterialTheme {
                Surface {
                    // TODO: Replace with AppNavGraph(appContainer) in Task 9.1
                    Text("Smart Shopping - A0 Bootstrap")
                }
            }
        }
    }
}
