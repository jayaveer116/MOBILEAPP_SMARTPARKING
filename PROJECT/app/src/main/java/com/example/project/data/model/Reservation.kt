package com.example.project.data.model

import com.google.gson.annotations.SerializedName

data class Reservation(
    @SerializedName("reservationId") val reservationId: String,
    @SerializedName("lotId") val lotId: String,
    @SerializedName("lotName") val lotName: String,
    @SerializedName("spotNumber") val spotNumber: String,
    @SerializedName("vehiclePlate") val vehiclePlate: String,
    @SerializedName("startTimeMillis") val startTimeMillis: Long,
    @SerializedName("durationHours") val durationHours: Int,
    @SerializedName("totalPrice") val totalPrice: Double,
    @SerializedName("status") var status: ReservationStatus? = ReservationStatus.ACTIVE,
    @SerializedName("qrPassCode") val qrPassCode: String,
    @SerializedName("latitude") val latitude: Double = 0.0,
    @SerializedName("longitude") val longitude: Double = 0.0
) {
    val currentStatus: ReservationStatus
        get() = status ?: ReservationStatus.ACTIVE

    val endTimeMillis: Long
        get() = startTimeMillis + (durationHours * 3600 * 1000L)

    val isExpired: Boolean
        get() = System.currentTimeMillis() > endTimeMillis
}

enum class ReservationStatus {
    @SerializedName("ACTIVE") ACTIVE,
    @SerializedName("COMPLETED") COMPLETED,
    @SerializedName("CANCELLED") CANCELLED
}
