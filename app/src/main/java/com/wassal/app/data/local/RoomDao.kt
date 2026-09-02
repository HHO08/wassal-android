package com.wassal.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wassal.app.data.model.Room

@Dao
interface RoomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(room: Room)

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getRoom(id: String): Room?

    @Query("SELECT * FROM rooms ORDER BY createdAt DESC")
    fun observeRooms(): kotlinx.coroutines.flow.Flow<List<Room>>
}
