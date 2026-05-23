// PATH: nw-parent-app/app/src/main/java/com/nw/parentalcontrol/ui/MainActivity.kt
package com.nw.parentalcontrol.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nw.parentalcontrol.ui.screens.DashboardScreen
import com.nw.parentalcontrol.ui.screens.PairingScreen
import com.nw.parentalcontrol.ui.theme.NWParentalTheme
import com.nw.parentalcontrol.viewmodel.ParentViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ParentViewModel by viewModels()

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            NWParentalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val uiState by viewModel.uiState.collectAsState()

                    // Decide start destination based on whether a device is already connected
                    val startDest = if (uiState.connectedDevice != null) "dashboard" else "pairing"

                    NavHost(navController = navController, startDestination = startDest) {
                        composable("pairing") {
                            PairingScreen(
                                viewModel  = viewModel,
                                onConnected = {
                                    navController.navigate("dashboard") {
                                        popUpTo("pairing") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel     = viewModel,
                                onDisconnected = {
                                    navController.navigate("pairing") {
                                        popUpTo("dashboard") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}