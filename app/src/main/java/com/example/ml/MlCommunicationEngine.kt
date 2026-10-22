package com.example.ml

import com.example.data.model.MlCommProbability
import com.example.data.model.QualityLevel
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

object MlCommunicationEngine {

    /**
     * Estimates distance from RSSI using standard Log-Distance Path Loss model:
     * RSSI = -(10 * n * log10(d) - A)
     * d = 10 ^ ((A - RSSI) / (10 * n))
     * where A is reference RSSI at 1m (approx -59 dBm for BLE/BT) and n is path loss exponent (2.0 in free space, 2.8 indoors).
     */
    fun estimateDistance(rssi: Int, pathLossExponent: Double = 2.4): Double {
        if (rssi == 0) return -1.0
        val txPowerAt1Meter = -59.0
        val ratio = (txPowerAt1Meter - rssi) / (10.0 * pathLossExponent)
        val distance = 10.0.pow(ratio)
        return (distance * 10).roundToInt() / 10.0
    }

    /**
     * ML Sigmoid logistic model predicting packet delivery & communication success chance (0 - 100%).
     * P = 1 / (1 + exp(-k * (RSSI - threshold))) * batteryPenalty * distancePenalty
     */
    fun computeCommunicationProbability(
        peerAddress: String,
        rssi: Int,
        batteryLevel: Int = 85,
        estimatedDistanceMeters: Double = 5.0
    ): MlCommProbability {
        // Normalize RSSI: -40 is top (100%), -95 is threshold (5%), -105 is dead zone (0%)
        val clampedRssi = min(max(rssi, -110), -35)
        
        // Logistic regression score
        // Midpoint around -75 dBm
        val z = (clampedRssi - (-75.0)) / 9.0
        val rawSigmoid = 1.0 / (1.0 + exp(-z))

        // Distance factor: Bluetooth range decays sharply past 15m, near impossible past 45m without relays
        val distanceFactor = when {
            estimatedDistanceMeters <= 5.0 -> 1.0
            estimatedDistanceMeters <= 15.0 -> 0.90
            estimatedDistanceMeters <= 25.0 -> 0.75
            estimatedDistanceMeters <= 40.0 -> 0.50
            else -> 0.25
        }

        // Battery factor: below 15%, device radio throttles BLE/RF power output
        val batteryFactor = when {
            batteryLevel > 30 -> 1.0
            batteryLevel > 15 -> 0.88
            else -> 0.70
        }

        val finalScore = (rawSigmoid * distanceFactor * batteryFactor * 100.0)
            .coerceIn(2.0, 99.0)
            .roundToInt()

        val rssiPercent = ((clampedRssi - (-105.0)) / 70.0 * 100.0).roundToInt().coerceIn(0, 100)
        val packetLossRisk = (100 - finalScore).coerceIn(1, 98)

        val (qualityLevel, retries, advice) = when {
            finalScore >= 80 -> Triple(
                QualityLevel.OPTIMAL,
                1,
                "Strong link. High bandwidth text & telemetry supported."
            )
            finalScore >= 55 -> Triple(
                QualityLevel.MODERATE,
                2,
                "Acceptable link. Standard text messages deliver reliably."
            )
            finalScore >= 30 -> Triple(
                QualityLevel.DEGRADED,
                4,
                "Weak signal. Use concise emergency broadcasts and avoid packet bursts."
            )
            else -> Triple(
                QualityLevel.CRITICAL_LOSS,
                6,
                "Critical signal boundary. Move closer or raise device above obstacles."
            )
        }

        val estimatedBitrate = when (qualityLevel) {
            QualityLevel.OPTIMAL -> 96.0
            QualityLevel.MODERATE -> 48.0
            QualityLevel.DEGRADED -> 12.0
            QualityLevel.CRITICAL_LOSS -> 2.4
        }

        return MlCommProbability(
            peerAddress = peerAddress,
            overallChancePercent = finalScore,
            rssiFactorPercent = rssiPercent,
            distanceMeters = estimatedDistanceMeters,
            estimatedBitrateKbps = estimatedBitrate,
            packetLossRiskPercent = packetLossRisk,
            recommendedRetries = retries,
            qualityLevel = qualityLevel,
            adviceNotice = advice
        )
    }
}
