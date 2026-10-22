package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey
    val userId: String = "local_user_1",
    val fullName: String = "Survivor / Responder",
    val emergencyContactName: String = "Emergency Contact",
    val emergencyContactPhone: String = "911",
    val bloodType: String = "O+",
    val medicalNotes: String = "No known allergies. Basic first-aid trained.",
    val organizationOrRole: String = "Civilian / Community Volunteer",
    val beaconBroadcastName: String = "ASVASA-Node-01",
    val isSharingLocation: Boolean = true
)
