package com.example.project.data.repository

import com.example.project.data.api.ApiClient
import com.example.project.data.api.ParkingApiService
import com.example.project.data.api.ReservationRequest
import com.example.project.data.model.OccupancyPrediction
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.ParkingSpot
import com.example.project.data.model.Reservation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ParkingRepository(
    private val apiService: ParkingApiService = ApiClient.service
) {

    suspend fun getParkingLots(): Result<List<ParkingLot>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getParkingLots()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch parking lots: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getParkingSpots(lotId: String): Result<List<ParkingSpot>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getParkingSpots(lotId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch spots: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReservation(
        lotId: String,
        spotNumber: String,
        vehiclePlate: String,
        durationHours: Int
    ): Result<Reservation> = withContext(Dispatchers.IO) {
        try {
            val req = ReservationRequest(
                lotId = lotId,
                spotNumber = spotNumber,
                vehiclePlate = vehiclePlate,
                durationHours = durationHours
            )
            val response = apiService.createReservation(req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create reservation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserReservations(): Result<List<Reservation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUserReservations()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch reservations: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelReservation(reservationId: String): Result<Reservation> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.cancelReservation(reservationId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to cancel reservation: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOccupancyPrediction(lotId: String): Result<OccupancyPrediction> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getOccupancyPrediction(lotId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch prediction: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
