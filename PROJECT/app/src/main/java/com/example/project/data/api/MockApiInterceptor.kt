package com.example.project.data.api

import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.UUID

class MockApiInterceptor : Interceptor {

    private val gson = Gson()

    // Dynamic state stored in memory for real-time simulation
    private val parkingLots = mutableListOf(
        MockData.lot1,
        MockData.lot2,
        MockData.lot3,
        MockData.lot4,
        MockData.lot5
    )

    private val userReservations = mutableListOf<Map<String, Any>>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        val path = request.url.encodedPath
        val method = request.method

        // Simulate network latency (200ms)
        Thread.sleep(150)

        val jsonResponse: String = when {
            // GET /api/v1/parking/lots
            method == "GET" && path.endsWith("/api/v1/parking/lots") -> {
                gson.toJson(parkingLots)
            }

            // GET /api/v1/parking/lots/{id}
            method == "GET" && path.matches(Regex(".*/api/v1/parking/lots/lot_[0-9]+$")) -> {
                val lotId = path.substringAfterLast("/")
                val lot = parkingLots.find { it["id"] == lotId } ?: parkingLots.first()
                gson.toJson(lot)
            }

            // GET /api/v1/parking/lots/{id}/spots
            method == "GET" && path.contains("/spots") -> {
                val lotId = path.split("/").getOrNull(5) ?: "lot_1"
                gson.toJson(MockData.generateSpotsForLot(lotId))
            }

            // POST /api/v1/parking/reservations
            method == "POST" && path.endsWith("/api/v1/parking/reservations") -> {
                val reqBody = request.body
                var lotId = "lot_1"
                var spotNum = "A-04"
                var plate = "ABC-1234"
                var duration = 2

                if (reqBody != null) {
                    val buffer = okio.Buffer()
                    reqBody.writeTo(buffer)
                    val bodyString = buffer.readUtf8()
                    try {
                        val map = gson.fromJson(bodyString, Map::class.java)
                        lotId = map["lotId"]?.toString() ?: lotId
                        spotNum = map["spotNumber"]?.toString() ?: spotNum
                        plate = map["vehiclePlate"]?.toString() ?: plate
                        duration = map["durationHours"]?.toString()?.toDoubleOrNull()?.toInt() ?: 2
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val lot = parkingLots.find { it["id"] == lotId } ?: parkingLots.first()
                val rate = (lot["hourlyRate"] as? Number)?.toDouble() ?: 4.5
                val lotName = (lot["name"] as? String) ?: "Central Parking"
                val resId = "RES-${UUID.randomUUID().toString().take(8).uppercase()}"
                val qrCode = "PARK-$resId-$spotNum"

                // Update lot availability
                val avail = (lot["availableSpots"] as? Double)?.toInt() ?: 10
                if (avail > 0) {
                    val updatedLot = lot.toMutableMap()
                    updatedLot["availableSpots"] = avail - 1
                    val idx = parkingLots.indexOfFirst { it["id"] == lotId }
                    if (idx != -1) parkingLots[idx] = updatedLot
                }

                val reservationMap = mapOf(
                    "reservationId" to resId,
                    "lotId" to lotId,
                    "lotName" to lotName,
                    "spotNumber" to spotNum,
                    "vehiclePlate" to plate,
                    "startTimeMillis" to System.currentTimeMillis(),
                    "durationHours" to duration,
                    "totalPrice" to (rate * duration),
                    "status" to "ACTIVE",
                    "qrPassCode" to qrCode,
                    "latitude" to (lot["latitude"] as Double),
                    "longitude" to (lot["longitude"] as Double)
                )

                userReservations.add(0, reservationMap)
                gson.toJson(reservationMap)
            }

            // GET /api/v1/parking/reservations
            method == "GET" && path.endsWith("/api/v1/parking/reservations") -> {
                gson.toJson(userReservations)
            }

            // POST /api/v1/parking/reservations/{id}/cancel
            method == "POST" && path.contains("/cancel") -> {
                val resId = path.split("/").getOrNull(5) ?: ""
                val resIdx = userReservations.indexOfFirst { it["reservationId"] == resId }
                if (resIdx != -1) {
                    val res = userReservations[resIdx].toMutableMap()
                    res["status"] = "CANCELLED"
                    userReservations[resIdx] = res

                    // Restore available spot
                    val lotId = res["lotId"] as? String
                    val lot = parkingLots.find { it["id"] == lotId }
                    if (lot != null) {
                        val avail = (lot["availableSpots"] as? Double)?.toInt() ?: 10
                        val updatedLot = lot.toMutableMap()
                        updatedLot["availableSpots"] = avail + 1
                        val idx = parkingLots.indexOfFirst { it["id"] == lotId }
                        if (idx != -1) parkingLots[idx] = updatedLot
                    }
                    gson.toJson(res)
                } else {
                    gson.toJson(mapOf("error" to "Reservation not found"))
                }
            }

            // GET /api/v1/parking/predictions/{id}
            method == "GET" && path.contains("/predictions/") -> {
                val lotId = path.substringAfterLast("/")
                val lot = parkingLots.find { it["id"] == lotId } ?: parkingLots.first()
                gson.toJson(MockData.generatePredictionForLot(lotId, lot["name"] as String))
            }

            else -> gson.toJson(mapOf("status" to "OK", "message" to "Smart Parking REST API operational"))
        }

        return Response.Builder()
            .code(200)
            .message("OK")
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .body(jsonResponse.toResponseBody("application/json".toMediaTypeOrNull()))
            .addHeader("content-type", "application/json")
            .build()
    }
}

object MockData {
    val lot1 = mapOf(
        "id" to "lot_1",
        "name" to "Downtown Grand Plaza Garage",
        "address" to "120 Market Street, City Center",
        "latitude" to 37.7892,
        "longitude" to -122.4014,
        "totalSpots" to 120,
        "availableSpots" to 42,
        "hourlyRate" to 5.00,
        "hasEvCharging" to true,
        "isCovered" to true,
        "hasHandicap" to true,
        "rating" to 4.8,
        "imageUrl" to "garage_1"
    )

    val lot2 = mapOf(
        "id" to "lot_2",
        "name" to "Metro Central Station Parking",
        "address" to "450 Mission St, Financial District",
        "latitude" to 37.7915,
        "longitude" to -122.3980,
        "totalSpots" to 85,
        "availableSpots" to 18,
        "hourlyRate" to 3.50,
        "hasEvCharging" to false,
        "isCovered" to true,
        "hasHandicap" to true,
        "rating" to 4.2,
        "imageUrl" to "garage_2"
    )

    val lot3 = mapOf(
        "id" to "lot_3",
        "name" to "Silicon Tech Hub Eco Park",
        "address" to "888 Howard Street, SoMa",
        "latitude" to 37.7825,
        "longitude" to -122.4048,
        "totalSpots" to 200,
        "availableSpots" to 115,
        "hourlyRate" to 6.00,
        "hasEvCharging" to true,
        "isCovered" to true,
        "hasHandicap" to true,
        "rating" to 4.9,
        "imageUrl" to "garage_3"
    )

    val lot4 = mapOf(
        "id" to "lot_4",
        "name" to "Bayfront Promenade Open Lot",
        "address" to "150 Embarcadero Pier",
        "latitude" to 37.7950,
        "longitude" to -122.3930,
        "totalSpots" to 60,
        "availableSpots" to 5,
        "hourlyRate" to 2.50,
        "hasEvCharging" to false,
        "isCovered" to false,
        "hasHandicap" to true,
        "rating" to 3.9,
        "imageUrl" to "garage_4"
    )

    val lot5 = mapOf(
        "id" to "lot_5",
        "name" to "Civic Center Express Parking",
        "address" to "200 McAllister St, Civic Center",
        "latitude" to 37.7800,
        "longitude" to -122.4170,
        "totalSpots" to 90,
        "availableSpots" to 62,
        "hourlyRate" to 4.00,
        "hasEvCharging" to true,
        "isCovered" to false,
        "hasHandicap" to true,
        "rating" to 4.4,
        "imageUrl" to "garage_5"
    )

    fun generateSpotsForLot(lotId: String): List<Map<String, Any>> {
        val spots = mutableListOf<Map<String, Any>>()
        val sections = listOf("A", "B", "C")
        for (sec in sections) {
            for (num in 1..10) {
                val spotNum = "$sec-${String.format("%02d", num)}"
                val isEv = (sec == "A" && num <= 3)
                val isHandicap = (sec == "A" && num in 4..5)
                val status = when {
                    num % 3 == 0 -> "OCCUPIED"
                    num == 7 -> "RESERVED"
                    else -> "AVAILABLE"
                }
                spots.add(
                    mapOf(
                        "spotId" to "${lotId}_$spotNum",
                        "lotId" to lotId,
                        "spotNumber" to spotNum,
                        "floor" to if (sec == "A") "Floor 1" else "Floor 2",
                        "section" to sec,
                        "status" to status,
                        "isEvSpot" to isEv,
                        "isHandicap" to isHandicap,
                        "pricePerHour" to 4.50
                    )
                )
            }
        }
        return spots
    }

    fun generatePredictionForLot(lotId: String, lotName: String): Map<String, Any> {
        val hourly = mutableListOf<Map<String, Any>>()
        val baseCurve = intArrayOf(
            15, 10, 8, 5, 10, 25, 45, 70, 88, 92, 85, 80,
            82, 86, 90, 88, 75, 65, 50, 40, 30, 25, 20, 15
        )
        for (h in 0..23) {
            val occ = (baseCurve[h] + (lotId.hashCode() % 10)).coerceIn(5, 98)
            hourly.add(
                mapOf(
                    "hourOfDay" to h,
                    "predictedOccupancyPercent" to occ,
                    "confidence" to 0.92
                )
            )
        }
        return mapOf(
            "lotId" to lotId,
            "lotName" to lotName,
            "hourlyPredictions" to hourly,
            "peakHoursRange" to "08:00 AM - 11:30 AM & 02:00 PM - 05:00 PM",
            "recommendedArrivalTime" to "Before 08:00 AM or 01:00 PM - 02:00 PM",
            "riskLevel" to "MODERATE"
        )
    }
}
