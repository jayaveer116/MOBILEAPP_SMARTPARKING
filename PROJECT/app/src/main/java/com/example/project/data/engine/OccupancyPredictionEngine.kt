package com.example.project.data.engine

import com.example.project.data.model.HourlyPrediction
import com.example.project.data.model.OccupancyPrediction
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.RiskLevel
import java.util.Calendar

object OccupancyPredictionEngine {

    /**
     * Research Algorithm: Occupancy Prediction Model
     * Calculates predicted occupancy percentage for a target hour offset based on:
     * 1. Baseline diurnal demand curve (24-hour distribution)
     * 2. Current live occupancy deviation factor
     * 3. Day of week demand multiplier (Weekday peak vs Weekend)
     * 4. Time decay factor lambda
     */
    fun predictOccupancyForLot(
        lot: ParkingLot,
        targetHourOffset: Int = 0
    ): OccupancyPrediction {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
        val dayMultiplier = if (isWeekend) 0.75 else 1.10

        // Diurnal baseline curve (00:00 to 23:00)
        val baseCurve = intArrayOf(
            15, 10, 8, 5, 8, 22, 45, 72, 88, 94, 88, 82,
            84, 88, 92, 90, 80, 68, 52, 42, 32, 25, 20, 15
        )

        val currentOccupancyPercent = lot.occupancyPercentage
        val currentBase = baseCurve[currentHour]
        val currentDrift = currentOccupancyPercent - currentBase

        val hourlyList = mutableListOf<HourlyPrediction>()

        for (h in 0..23) {
            val hourDiff = (h - currentHour + 24) % 24
            // Exponential time decay for live drift effect
            val decay = Math.exp(-0.15 * hourDiff)
            val predictedPercent = ((baseCurve[h] * dayMultiplier) + (currentDrift * decay))
                .toInt()
                .coerceIn(5, 98)

            val confidence = (0.95 - (hourDiff * 0.015)).coerceAtLeast(0.70)

            hourlyList.add(
                HourlyPrediction(
                    hourOfDay = h,
                    predictedOccupancyPercent = predictedPercent,
                    confidence = (confidence * 100).toInt() / 100.0
                )
            )
        }

        val targetHour = (currentHour + targetHourOffset) % 24
        val targetPrediction = hourlyList.find { it.hourOfDay == targetHour }?.predictedOccupancyPercent ?: currentOccupancyPercent

        val risk = when {
            targetPrediction >= 85 -> RiskLevel.HIGH
            targetPrediction >= 60 -> RiskLevel.MODERATE
            else -> RiskLevel.LOW
        }

        val peakStart = hourlyList.filter { it.predictedOccupancyPercent > 80 }.minOfOrNull { it.hourOfDay } ?: 9
        val peakEnd = hourlyList.filter { it.predictedOccupancyPercent > 80 }.maxOfOrNull { it.hourOfDay } ?: 17

        val peakRangeStr = String.format("%02d:00 - %02d:00", peakStart, peakEnd)
        val recommendedTime = if (risk == RiskLevel.HIGH) "Arrive before ${peakStart}:00 AM or after ${peakEnd}:00 PM" else "Good availability expected"

        return OccupancyPrediction(
            lotId = lot.id,
            lotName = lot.name,
            hourlyPredictions = hourlyList,
            peakHoursRange = peakRangeStr,
            recommendedArrivalTime = recommendedTime,
            riskLevel = risk
        )
    }
}
