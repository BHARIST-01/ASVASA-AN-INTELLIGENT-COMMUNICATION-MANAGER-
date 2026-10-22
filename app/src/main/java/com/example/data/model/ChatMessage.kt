package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MessageStatus {
    PENDING,
    SENT,
    DELIVERED,
    FAILED
}

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey
    val id: String,
    val peerAddress: String, // Destination address or "BROADCAST"
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean,
    val isEmergencyAlert: Boolean = false,
    val deliveryProbability: Int = 100, // ML estimated chance when sent
    val status: MessageStatus = MessageStatus.SENT
)
