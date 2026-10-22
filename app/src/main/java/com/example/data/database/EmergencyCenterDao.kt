package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.EmergencyCenter
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyCenterDao {
    @Query("SELECT * FROM emergency_centers ORDER BY distanceKm ASC")
    fun getAllEmergencyCenters(): Flow<List<EmergencyCenter>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(centers: List<EmergencyCenter>)

    @Query("SELECT COUNT(*) FROM emergency_centers")
    suspend fun getCount(): Int
}
