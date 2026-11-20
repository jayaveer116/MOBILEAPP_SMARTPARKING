package com.example.project.data.model

import com.google.gson.annotations.SerializedName

data class ParkingLot(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("totalSpots") val totalSpots: Int,
    @SerializedName("availableSpots") val availableSpots: Int,
    @SerializedName("hourlyRate") val hourlyRate: Double,
    @SerializedName("hasEvCharging") val hasEvCharging: Boolean = false,
    @SerializedName("isCovered") val isCovered: Boolean = false,
    @SerializedName("hasHandicap") val hasHandicap: Boolean = false,
    @SerializedName("rating") val rating: Double = 4.5,
    @SerializedName("imageUrl") val imageUrl: String? = "",
    // Calculated fields
    var distanceKm: Double = 0.0,
    var etaMinutes: Int = 0,
    var recommendationScore: Int = 0,
    var matchReason: String? = ""
) {
    val occupancyPercentage: Int
        get() = if (totalSpots > 0) (((totalSpots - availableSpots).toDouble() / totalSpots) * 100).toInt() else 0

    val occupancyStatus: OccupancyStatus
        get() = when {
            availableSpots <= 2 -> OccupancyStatus.FULL
            availableSpots <= (totalSpots * 0.2) -> OccupancyStatus.HIGH
            availableSpots <= (totalSpots * 0.5) -> OccupancyStatus.MODERATE
            else -> OccupancyStatus.LOW
        }

    val displayMatchReason: String
        get() = if (!matchReason.isNullOrBlank()) matchReason!! else "Good general parking match"
}

enum class OccupancyStatus {
    LOW,        // Green: Plenty of space (>50% free)
    MODERATE,   // Yellow: Moderately full (20-50% free)
    HIGH,       // Orange: Almost full (<20% free)
    FULL        // Red: Full / <= 2 spots
}
