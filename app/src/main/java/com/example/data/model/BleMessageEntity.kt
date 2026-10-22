package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ble_messages",
    indices = [
        Index(value = ["recipientAddress"]),
        Index(value = ["senderAddress"]),
        Index(value = ["timestamp"])
    ]
)
data class BleMessageEntity(
    @PrimaryKey
    val id: String,
    val senderAddress: String,
    val senderName: String,
    val recipientAddress: String, // Destination BLE MAC or "BROADCAST"
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean,
    val isEmergencySos: Boolean = false,
    val rssiAtTransmission: Int = -65,
    val deliveryProbabilityPercent: Int = 85,
    val deliveryStatus: String = "DELIVERED",
    val serviceUuid: String = "0000a5a5-0000-1000-8000-00805f9b34fb",
    val packetSizeBytes: Int = 0
)
