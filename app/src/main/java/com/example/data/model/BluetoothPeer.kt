package com.example.data.model

data class BluetoothPeer(
    val address: String,
    val name: String,
    val rssi: Int, // e.g. -50 to -95 dBm
    val estimatedDistanceMeters: Double,
    val batteryLevel: Int = 85, // 0-100%
    val commProbabilityPercent: Int, // 0-100% computed by ML engine
    val isConnected: Boolean = false,
    val isDisasterBeacon: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)
