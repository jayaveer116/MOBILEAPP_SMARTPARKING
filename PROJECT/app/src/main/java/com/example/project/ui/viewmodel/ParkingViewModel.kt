package com.example.project.ui.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.project.data.engine.OccupancyPredictionEngine
import com.example.project.data.engine.SmartRecommendationEngine
import com.example.project.data.model.OccupancyPrediction
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.ParkingSpot
import com.example.project.data.model.Reservation
import com.example.project.data.model.UserPreferences
import com.example.project.data.repository.ParkingRepository
import com.example.project.location.GPSLocationManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ParkingRepository()
    val gpsLocationManager = GPSLocationManager(application)

    private val _userLocation = MutableLiveData<Location>()
    val userLocation: LiveData<Location> = _userLocation

    private val _parkingLots = MutableLiveData<List<ParkingLot>>(emptyList())
    val parkingLots: LiveData<List<ParkingLot>> = _parkingLots

    private val _recommendedLots = MutableLiveData<List<ParkingLot>>(emptyList())
    val recommendedLots: LiveData<List<ParkingLot>> = _recommendedLots

    private val _selectedLot = MutableLiveData<ParkingLot?>()
    val selectedLot: LiveData<ParkingLot?> = _selectedLot

    private val _spotsForSelectedLot = MutableLiveData<List<ParkingSpot>>(emptyList())
    val spotsForSelectedLot: LiveData<List<ParkingSpot>> = _spotsForSelectedLot

    private val _userReservations = MutableLiveData<List<Reservation>>(emptyList())
    val userReservations: LiveData<List<Reservation>> = _userReservations

    private val _selectedPrediction = MutableLiveData<OccupancyPrediction?>()
    val selectedPrediction: LiveData<OccupancyPrediction?> = _selectedPrediction

    private val _userPreferences = MutableLiveData(UserPreferences())
    val userPreferences: LiveData<UserPreferences> = _userPreferences

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _statusMessage = MutableLiveData<String?>()
    val statusMessage: LiveData<String?> = _statusMessage

    init {
        observeGpsLocation()
        fetchParkingLots()
        fetchUserReservations()
    }

    private fun observeGpsLocation() {
        viewModelScope.launch {
            gpsLocationManager.startLocationUpdates()
            gpsLocationManager.currentLocation.collectLatest { loc ->
                _userLocation.value = loc
                updateDistancesAndRecommendations(loc)
            }
        }
    }

    fun fetchParkingLots() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getParkingLots()
            _isLoading.value = false
            result.onSuccess { lots ->
                _parkingLots.value = lots
                _userLocation.value?.let { updateDistancesAndRecommendations(it) }
            }.onFailure { err ->
                _statusMessage.value = "Error fetching parking lots: ${err.message}"
            }
        }
    }

    fun selectParkingLot(lot: ParkingLot) {
        _selectedLot.value = lot
        fetchSpotsForLot(lot.id)
        fetchPredictionForLot(lot)
    }

    fun clearSelectedLot() {
        _selectedLot.value = null
        _spotsForSelectedLot.value = emptyList()
    }

    fun fetchSpotsForLot(lotId: String) {
        viewModelScope.launch {
            val result = repository.getParkingSpots(lotId)
            result.onSuccess { spots ->
                _spotsForSelectedLot.value = spots
            }.onFailure { err ->
                _statusMessage.value = "Error fetching spots: ${err.message}"
            }
        }
    }

    fun createReservation(
        lotId: String,
        spotNumber: String,
        vehiclePlate: String,
        durationHours: Int
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.createReservation(lotId, spotNumber, vehiclePlate, durationHours)
            _isLoading.value = false
            result.onSuccess { reservation ->
                _statusMessage.value = "Reservation successful! Spot ${reservation.spotNumber} booked."
                fetchUserReservations()
                fetchParkingLots() // Refresh lot availability
            }.onFailure { err ->
                _statusMessage.value = "Booking failed: ${err.message}"
            }
        }
    }

    fun fetchUserReservations() {
        viewModelScope.launch {
            val result = repository.getUserReservations()
            result.onSuccess { list ->
                _userReservations.value = list
            }.onFailure { err ->
                _statusMessage.value = "Failed to load reservations"
            }
        }
    }

    fun cancelReservation(reservationId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.cancelReservation(reservationId)
            _isLoading.value = false
            result.onSuccess {
                _statusMessage.value = "Reservation cancelled"
                fetchUserReservations()
                fetchParkingLots()
            }.onFailure { err ->
                _statusMessage.value = "Cancellation failed: ${err.message}"
            }
        }
    }

    fun fetchPredictionForLot(lot: ParkingLot) {
        viewModelScope.launch {
            val localPrediction = OccupancyPredictionEngine.predictOccupancyForLot(lot)
            _selectedPrediction.value = localPrediction

            val result = repository.getOccupancyPrediction(lot.id)
            result.onSuccess { remotePrediction ->
                _selectedPrediction.value = remotePrediction
            }
        }
    }

    fun updatePreferences(prefs: UserPreferences) {
        _userPreferences.value = prefs
        _userLocation.value?.let { updateDistancesAndRecommendations(it) }
    }

    private fun updateDistancesAndRecommendations(loc: Location) {
        val currentLots = _parkingLots.value ?: return
        val prefs = _userPreferences.value ?: UserPreferences()

        val updatedLots = currentLots.map { lot ->
            val dist = GPSLocationManager.calculateDistanceKm(
                loc.latitude, loc.longitude,
                lot.latitude, lot.longitude
            )
            val eta = GPSLocationManager.calculateEtaMinutes(dist)
            lot.copy(distanceKm = dist, etaMinutes = eta)
        }

        _parkingLots.value = updatedLots

        // Run MCDA Recommendation Engine
        val ranked = SmartRecommendationEngine.rankParkingLots(updatedLots, prefs)
        _recommendedLots.value = ranked
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        gpsLocationManager.stopLocationUpdates()
    }
}
