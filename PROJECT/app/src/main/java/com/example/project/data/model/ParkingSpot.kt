package com.example.project.data.model

import com.google.gson.annotations.SerializedName

data class ParkingSpot(
    @SerializedName("spotId") val spotId: String,
    @SerializedName("lotId") val lotId: String,
    @SerializedName("spotNumber") val spotNumber: String,
    @SerializedName("floor") val floor: String? = "1",
    @SerializedName("section") val section: String? = "A",
    @SerializedName("status") var status: SpotStatus? = SpotStatus.AVAILABLE,
    @SerializedName("isEvSpot") val isEvSpot: Boolean = false,
    @SerializedName("isHandicap") val isHandicap: Boolean = false,
    @SerializedName("pricePerHour") val pricePerHour: Double = 0.0
) {
    val displayFloor: String
        get() = floor ?: "Floor 1"

    val displaySection: String
        get() = section ?: "A"

    val currentStatus: SpotStatus
        get() = status ?: SpotStatus.AVAILABLE
}

enum class SpotStatus {
    @SerializedName("AVAILABLE") AVAILABLE,
    @SerializedName("OCCUPIED") OCCUPIED,
    @SerializedName("RESERVED") RESERVED
}
