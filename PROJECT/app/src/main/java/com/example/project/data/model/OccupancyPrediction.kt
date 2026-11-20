package com.example.project.data.model

import com.google.gson.annotations.SerializedName

data class HourlyPrediction(
    @SerializedName("hourOfDay") val hourOfDay: Int, // 0 to 23
    @SerializedName("predictedOccupancyPercent") val predictedOccupancyPercent: Int,
    @SerializedName("confidence") val confidence: Double // 0.0 to 1.0
)

data class OccupancyPrediction(
    @SerializedName("lotId") val lotId: String,
    @SerializedName("lotName") val lotName: String,
    @SerializedName("hourlyPredictions") val hourlyPredictions: List<HourlyPrediction>,
    @SerializedName("peakHoursRange") val peakHoursRange: String,
    @SerializedName("recommendedArrivalTime") val recommendedArrivalTime: String,
    @SerializedName("riskLevel") val riskLevel: RiskLevel
)

enum class RiskLevel {
    @SerializedName("LOW") LOW,          // High spot availability expected
    @SerializedName("MODERATE") MODERATE, // Moderate crowding
    @SerializedName("HIGH") HIGH          // High risk of no available spots
}
