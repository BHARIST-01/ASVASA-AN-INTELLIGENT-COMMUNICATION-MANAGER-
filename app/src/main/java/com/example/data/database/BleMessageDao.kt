package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BleMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BleMessageDao {

    @Query("SELECT * FROM ble_messages ORDER BY timestamp ASC")
    fun getAllBleMessages(): Flow<List<BleMessageEntity>>

    @Query("SELECT * FROM ble_messages WHERE recipientAddress = :peerAddress OR senderAddress = :peerAddress OR recipientAddress = 'BROADCAST' ORDER BY timestamp ASC")
    fun getMessagesForPeer(peerAddress: String): Flow<List<BleMessageEntity>>

    @Query("SELECT * FROM ble_messages WHERE isEmergencySos = 1 ORDER BY timestamp DESC")
    fun getEmergencySosMessages(): Flow<List<BleMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBleMessage(message: BleMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBleMessages(messages: List<BleMessageEntity>)

    @Query("UPDATE ble_messages SET deliveryStatus = :status WHERE id = :messageId")
    suspend fun updateDeliveryStatus(messageId: String, status: String)

    @Query("SELECT COUNT(*) FROM ble_messages")
    suspend fun getMessageCount(): Int

    @Query("DELETE FROM ble_messages WHERE recipientAddress = :peerAddress OR senderAddress = :peerAddress")
    suspend fun deleteMessagesForPeer(peerAddress: String)

    @Query("DELETE FROM ble_messages")
    suspend fun clearAllBleMessages()
}
