package com.example.bluetooth

import android.os.ParcelUuid
import java.util.UUID

object BleConstants {
    // ASVASA Emergency Service UUID
    val ASVASA_SERVICE_UUID: UUID = UUID.fromString("0000a5a5-0000-1000-8000-00805f9b34fb")
    val ASVASA_PARCEL_UUID = ParcelUuid(ASVASA_SERVICE_UUID)

    // Characteristic for sending and receiving peer-to-peer text messages
    val MESSAGE_CHAR_UUID: UUID = UUID.fromString("0000a5a6-0000-1000-8000-00805f9b34fb")

    // Characteristic for node telemetry (battery, role, emergency status)
    val TELEMETRY_CHAR_UUID: UUID = UUID.fromString("0000a5a7-0000-1000-8000-00805f9b34fb")

    // Standard Client Characteristic Configuration Descriptor (CCCD)
    val CCCD_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val MAX_BLE_PACKET_BYTES = 512
}

enum class BleAdvertisingState {
    IDLE,
    STARTING,
    ADVERTISING,
    UNSUPPORTED,
    FAILED
}
