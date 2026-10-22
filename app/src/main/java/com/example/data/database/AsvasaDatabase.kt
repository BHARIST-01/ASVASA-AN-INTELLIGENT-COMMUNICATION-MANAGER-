package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BleMessageEntity
import com.example.data.model.ChatMessage
import com.example.data.model.EmergencyCenter
import com.example.data.model.UserProfile

@Database(
    entities = [UserProfile::class, ChatMessage::class, EmergencyCenter::class, BleMessageEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AsvasaDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun emergencyCenterDao(): EmergencyCenterDao
    abstract fun bleMessageDao(): BleMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AsvasaDatabase? = null

        fun getDatabase(context: Context): AsvasaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AsvasaDatabase::class.java,
                    "asvasa_emergency.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
