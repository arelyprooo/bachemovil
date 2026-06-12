package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.ReportRepository
import com.example.data.repository.UserRepository
import com.example.ui.BacheViewModel
import com.example.ui.Screen
import com.example.ui.ViewModelFactory
import com.example.ui.components.DashboardScreen
import com.example.ui.components.LoginScreen
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.location.LocationServices

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: BacheViewModel

    // Request permissions launcher for automatic GPS geolocation
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchDeviceLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Room Local Database
        val database = AppDatabase.getDatabase(applicationContext)

        // 2. Initialize Repositories
        val userRepository = UserRepository(database.userDao())
        val reportRepository = ReportRepository(database.reportDao())

        // 3. Setup ViewModel with custom Factory
        val factory = ViewModelFactory(application, userRepository, reportRepository)
        viewModel = ViewModelProvider(this, factory)[BacheViewModel::class.java]

        // 4. Ask for location permissions to auto-register bache GPS triggers
        checkLocationPermissions()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_navigation_fade"
                    ) { screen ->
                        when (screen) {
                            is Screen.Login, is Screen.Register -> {
                                LoginScreen(viewModel = viewModel)
                            }
                            is Screen.Dashboard -> {
                                DashboardScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            fetchDeviceLocation()
        }
    }

    private fun fetchDeviceLocation() {
        try {
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: android.location.Location? ->
                    if (location != null) {
                        viewModel.setLocation(location.latitude, location.longitude)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
