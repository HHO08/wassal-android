package com.myapp.p2p.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.myapp.p2p.data.model.ChatMessage
import com.myapp.p2p.data.model.Room
import com.myapp.p2p.data.model.UserProfile

@Database(
    entities = [UserProfile::class, ChatMessage::class, Room::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun messageDao(): MessageDao
    abstract fun roomDao(): RoomDao
}
