package com.example.project

import com.example.project.data.engine.OccupancyPredictionEngine
import com.example.project.data.engine.SmartRecommendationEngine
import com.example.project.data.model.ParkingLot
import com.example.project.data.model.UserPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OccupancyPredictionEngineTest {

    private val sampleLot = ParkingLot(
        id = "lot_test_1",
        name = "Grand Plaza Garage",
        address = "120 Market St",
        latitude = 37.7892,
        longitude = -122.4014,
        totalSpots = 100,
        availableSpots = 40,
        hourlyRate = 5.0,
        hasEvCharging = true,
        isCovered = true,
        hasHandicap = true,
        distanceKm = 0.8
    )

    @Test
    fun testOccupancyPredictionCurveGeneration() {
        val prediction = OccupancyPredictionEngine.predictOccupancyForLot(sampleLot, targetHourOffset = 0)

        assertNotNull(prediction)
        assertEquals("lot_test_1", prediction.lotId)
        assertEquals(24, prediction.hourlyPredictions.size)
        assertTrue(prediction.peakHoursRange.isNotEmpty())
        assertNotNull(prediction.riskLevel)
    }

    @Test
    fun testSmartRecommendationEngineScoring() {
        val lot2 = sampleLot.copy(
            id = "lot_test_2",
            name = "Eco Lot",
            hourlyRate = 2.0,
            hasEvCharging = true,
            distanceKm = 0.2
        )

        val prefs = UserPreferences(
            requireEv = true,
            maxHourlyRate = 10.0,
            priorityWeightDistance = 0.5
        )

        val ranked = SmartRecommendationEngine.rankParkingLots(listOf(sampleLot, lot2), prefs)

        assertEquals(2, ranked.size)
        assertTrue(ranked[0].recommendationScore > 0)
        assertTrue(ranked[0].displayMatchReason.isNotEmpty())
        // Cheaper and closer lot2 should rank higher
        assertEquals("lot_test_2", ranked[0].id)
    }
}
