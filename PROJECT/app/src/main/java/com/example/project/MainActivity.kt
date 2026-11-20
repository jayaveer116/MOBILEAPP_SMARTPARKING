package com.example.project

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.project.databinding.ActivityMainBinding
import com.example.project.ui.map.MapGuidanceFragment
import com.example.project.ui.prediction.OccupancyPredictionFragment
import com.example.project.ui.recommendation.SmartRecommenderFragment
import com.example.project.ui.reservations.ReservationsFragment
import com.example.project.ui.viewmodel.ParkingViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ParkingViewModel by viewModels()

    private val mapFragment by lazy { MapGuidanceFragment() }
    private val recommenderFragment by lazy { SmartRecommenderFragment() }
    private val reservationsFragment by lazy { ReservationsFragment() }
    private val predictionFragment by lazy { OccupancyPredictionFragment() }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.gpsLocationManager.startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkLocationPermissions()
        setupBottomNavigation()

        // Default to Map Fragment
        if (savedInstanceState == null) {
            switchFragment(mapFragment)
        }
    }

    private fun checkLocationPermissions() {
        val finePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (finePerm != PackageManager.PERMISSION_GRANTED && coarsePerm != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.gpsLocationManager.startLocationUpdates()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    switchFragment(mapFragment)
                    true
                }
                R.id.nav_recommendations -> {
                    switchFragment(recommenderFragment)
                    true
                }
                R.id.nav_reservations -> {
                    switchFragment(reservationsFragment)
                    true
                }
                R.id.nav_predictions -> {
                    switchFragment(predictionFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
