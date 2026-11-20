package com.example.project.data.api

import com.example.project.data.model.OccupancyPrediction
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.ParkingSpot
import com.example.project.data.model.Reservation
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ParkingApiService {

    @GET("api/v1/parking/lots")
    suspend fun getParkingLots(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("ev") ev: Boolean? = null,
        @Query("covered") covered: Boolean? = null,
        @Query("maxRate") maxRate: Double? = null
    ): Response<List<ParkingLot>>

    @GET("api/v1/parking/lots/{lotId}")
    suspend fun getParkingLotById(
        @Path("lotId") lotId: String
    ): Response<ParkingLot>

    @GET("api/v1/parking/lots/{lotId}/spots")
    suspend fun getParkingSpots(
        @Path("lotId") lotId: String
    ): Response<List<ParkingSpot>>

    @POST("api/v1/parking/reservations")
    suspend fun createReservation(
        @Body reservationRequest: ReservationRequest
    ): Response<Reservation>

    @GET("api/v1/parking/reservations")
    suspend fun getUserReservations(): Response<List<Reservation>>

    @POST("api/v1/parking/reservations/{reservationId}/cancel")
    suspend fun cancelReservation(
        @Path("reservationId") reservationId: String
    ): Response<Reservation>

    @GET("api/v1/parking/predictions/{lotId}")
    suspend fun getOccupancyPrediction(
        @Path("lotId") lotId: String
    ): Response<OccupancyPrediction>
}

data class ReservationRequest(
    val lotId: String,
    val spotNumber: String,
    val vehiclePlate: String,
    val durationHours: Int
)
