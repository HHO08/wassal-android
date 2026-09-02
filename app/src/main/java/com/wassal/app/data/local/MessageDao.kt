package com.wassal.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.wassal.app.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun observeByRoom(roomId: String): Flow<List<ChatMessage>>

    @Insert
    suspend fun insert(message: ChatMessage): Long

    @Query("DELETE FROM messages WHERE roomId = :roomId")
    suspend fun clearRoom(roomId: String)
}
