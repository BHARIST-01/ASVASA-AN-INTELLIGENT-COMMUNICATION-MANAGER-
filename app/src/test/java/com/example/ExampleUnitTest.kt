package com.example

import com.example.bluetooth.BleConstants
import com.example.data.gemini.EmergencyActionType
import com.example.data.gemini.GeminiRepository
import com.example.data.model.BleMessageEntity
import com.example.ml.MlCommunicationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBleInfrastructureConstants() {
        assertNotNull(BleConstants.ASVASA_SERVICE_UUID)
        assertNotNull(BleConstants.MESSAGE_CHAR_UUID)
        assertNotNull(BleConstants.CCCD_DESCRIPTOR_UUID)
        assertEquals("0000a5a5-0000-1000-8000-00805f9b34fb", BleConstants.ASVASA_SERVICE_UUID.toString())

        // Test payload serialization under MTU limit
        val sampleEmergencyMessage = "🚨 SOS DISTRESS: Survivor at Lat 37.7749, Lon -122.4194. Trapped in rubble, need assistance!"
        val bytes = sampleEmergencyMessage.toByteArray(Charsets.UTF_8)
        assertTrue(bytes.size < BleConstants.MAX_BLE_PACKET_BYTES)

        val restoredText = String(bytes, Charsets.UTF_8)
        assertEquals(sampleEmergencyMessage, restoredText)
    }

    @Test
    fun testBleMessageEntityOfflineProperties() {
        val message = BleMessageEntity(
            id = "test_ble_123",
            senderAddress = "02:00:AA:BB:CC:11",
            senderName = "RescueTeam-Alpha",
            recipientAddress = "LOCAL_NODE",
            messageText = "Rendezvous at zone B waypoint.",
            timestamp = 1698000000000L,
            isOutgoing = false,
            isEmergencySos = false,
            rssiAtTransmission = -68,
            deliveryProbabilityPercent = 88,
            deliveryStatus = "DELIVERED",
            packetSizeBytes = 30
        )

        assertEquals("test_ble_123", message.id)
        assertEquals("02:00:AA:BB:CC:11", message.senderAddress)
        assertEquals("RescueTeam-Alpha", message.senderName)
        assertEquals("LOCAL_NODE", message.recipientAddress)
        assertFalse(message.isOutgoing)
        assertFalse(message.isEmergencySos)
        assertEquals(88, message.deliveryProbabilityPercent)
        assertEquals("DELIVERED", message.deliveryStatus)
    }

    @Test
    fun testMlCommunicationProbabilityCalculation() {
        val closeProb = MlCommunicationEngine.computeCommunicationProbability(
            peerAddress = "02:00:AA:BB:CC:11",
            rssi = -55,
            batteryLevel = 90,
            estimatedDistanceMeters = 3.0
        )
        // High RSSI close distance should yield high communication chance (>80%)
        assertTrue("Expected close range probability > 80%", closeProb.overallChancePercent >= 80)

        val farProb = MlCommunicationEngine.computeCommunicationProbability(
            peerAddress = "02:00:AA:BB:CC:22",
            rssi = -95,
            batteryLevel = 10,
            estimatedDistanceMeters = 40.0
        )
        // Low RSSI far distance with low battery should yield degraded probability (< 45%)
        assertTrue("Expected far range probability < 45%", farProb.overallChancePercent < 45)
    }

    @Test
    fun testVoiceCommandParsing() {
        val geminiRepo = GeminiRepository()

        val policeCmd = geminiRepo.parseVoiceCommand("Please call police right now")
        assertEquals(EmergencyActionType.CALL_POLICE, policeCmd.actionType)
        assertEquals("100", policeCmd.immediatePhoneToDial)

        val fireCmd = geminiRepo.parseVoiceCommand("Fire brigade urgently needed")
        assertEquals(EmergencyActionType.CALL_FIRE, fireCmd.actionType)
        assertEquals("101", fireCmd.immediatePhoneToDial)

        val doctorCmd = geminiRepo.parseVoiceCommand("Call ambulance someone is injured")
        assertEquals(EmergencyActionType.CALL_AMBULANCE, doctorCmd.actionType)
        assertEquals("102", doctorCmd.immediatePhoneToDial)

        val sosCmd = geminiRepo.parseVoiceCommand("Send emergency SOS broadcast")
        assertEquals(EmergencyActionType.BROADCAST_SOS, sosCmd.actionType)
    }

    @Test
    fun testHaversineDistanceCalculation() {
        val lat1 = 37.7749
        val lon1 = -122.4194
        val lat2 = 37.7790
        val lon2 = -122.4150

        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        val distance = earthRadiusKm * c

        // Distance between these SF coordinates is approx 0.60 km
        assertTrue("Expected distance between 0.4 and 0.8 km", distance in 0.4..0.8)
    }
}
