package com.example.project.data.engine

import com.example.project.data.model.ParkingLot
import com.example.project.data.model.UserPreferences

object SmartRecommendationEngine {

    /**
     * Research Contribution: MCDA Recommendation System
     * Computes a weighted score S for each parking option:
     * S = w_dist * S_dist + w_avail * S_avail + w_price * S_price + S_amenity_bonus
     */
    fun rankParkingLots(
        lots: List<ParkingLot>,
        prefs: UserPreferences
    ): List<ParkingLot> {
        val maxDist = lots.maxOfOrNull { it.distanceKm }?.coerceAtLeast(1.0) ?: 5.0
        val maxPrice = lots.maxOfOrNull { it.hourlyRate }?.coerceAtLeast(1.0) ?: 10.0

        return lots.map { lot ->
            // Filter out lots violating strict requirements if any
            if (prefs.requireEv && !lot.hasEvCharging) {
                lot.recommendationScore = 0
                lot.matchReason = "Missing EV Charger"
                return@map lot
            }
            if (prefs.requireCovered && !lot.isCovered) {
                lot.recommendationScore = 0
                lot.matchReason = "Uncovered Lot"
                return@map lot
            }
            if (prefs.requireHandicap && !lot.hasHandicap) {
                lot.recommendationScore = 0
                lot.matchReason = "No Handicap Access"
                return@map lot
            }

            // 1. Distance Score (0.0 to 1.0) - Closer is higher
            val normDist = (1.0 - (lot.distanceKm / maxDist)).coerceIn(0.0, 1.0)

            // 2. Predicted Availability Score at arrival ETA
            val predObj = OccupancyPredictionEngine.predictOccupancyForLot(lot, prefs.targetArrivalTimeOffsetHours)
            val targetHour = (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) + prefs.targetArrivalTimeOffsetHours) % 24
            val predOccupancy = predObj.hourlyPredictions.find { it.hourOfDay == targetHour }?.predictedOccupancyPercent ?: lot.occupancyPercentage
            val normAvail = ((100 - predOccupancy) / 100.0).coerceIn(0.0, 1.0)

            // 3. Price Score (0.0 to 1.0) - Cheaper is higher
            val normPrice = (1.0 - (lot.hourlyRate / maxPrice)).coerceIn(0.0, 1.0)

            // Calculate Weighted Composite Score
            val wDist = prefs.priorityWeightDistance
            val wPrice = prefs.priorityWeightPrice
            val wAvail = prefs.priorityWeightAvailability

            var compositeScore = (wDist * normDist + wPrice * normPrice + wAvail * normAvail) * 100

            // Amenity Match Bonuses
            if (lot.hasEvCharging && prefs.requireEv) compositeScore += 10
            if (lot.isCovered && prefs.requireCovered) compositeScore += 5
            if (lot.rating >= 4.5) compositeScore += 5

            val finalScore = compositeScore.toInt().coerceIn(10, 99)
            lot.recommendationScore = finalScore

            // Generate intelligent match rationale description
            val reasons = mutableListOf<String>()
            if (normAvail >= 0.5) reasons.add("High predicted spot availability (${100 - predOccupancy}% free)")
            if (normDist >= 0.7) reasons.add("Short walk distance (${lot.distanceKm} km)")
            if (normPrice >= 0.6) reasons.add("Budget friendly ($${String.format("%.2f", lot.hourlyRate)}/hr)")
            if (lot.hasEvCharging) reasons.add("EV Fast Charger available")

            lot.matchReason = if (reasons.isNotEmpty()) reasons.take(2).joinToString(" • ") else "Good general parking match"

            lot
        }.sortedByDescending { it.recommendationScore }
    }
}
