package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EmergencyCenterType {
    POLICE,
    FIRE,
    HOSPITAL,
    DISASTER_RELIEF
}

@Entity(tableName = "emergency_centers")
data class EmergencyCenter(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: EmergencyCenterType,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double,
    val phoneNumber: String,
    val address: String,
    val open24Hours: Boolean = true,
    val specialCapabilities: String = "24/7 Dispatch & Emergency Response",
    val estimatedResponseTimeMinutes: Int = 8
)
