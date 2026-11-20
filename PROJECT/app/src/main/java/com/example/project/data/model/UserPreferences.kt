package com.example.project.data.model

data class UserPreferences(
    var requireEv: Boolean = false,
    var requireCovered: Boolean = false,
    var requireHandicap: Boolean = false,
    var maxHourlyRate: Double = 15.0,
    var maxWalkingDistanceKm: Double = 3.0,
    var targetArrivalTimeOffsetHours: Int = 0, // 0 = now, 1 = in 1 hour
    var durationHours: Int = 2,
    var priorityWeightDistance: Double = 0.3,
    var priorityWeightPrice: Double = 0.3,
    var priorityWeightAvailability: Double = 0.4
)
