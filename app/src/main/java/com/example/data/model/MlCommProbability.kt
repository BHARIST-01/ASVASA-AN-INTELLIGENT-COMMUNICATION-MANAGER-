package com.example.data.model

enum class QualityLevel {
    OPTIMAL,
    MODERATE,
    DEGRADED,
    CRITICAL_LOSS
}

data class MlCommProbability(
    val peerAddress: String,
    val overallChancePercent: Int, // 0 - 100%
    val rssiFactorPercent: Int,
    val distanceMeters: Double,
    val estimatedBitrateKbps: Double,
    val packetLossRiskPercent: Int,
    val recommendedRetries: Int,
    val qualityLevel: QualityLevel,
    val adviceNotice: String
)
