package com.myapp.p2p.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A call room. Contains the generated id plus the deep link used to join it.
 * Kept locally only.
 */
@Entity(tableName = "rooms")
data class Room(
    @PrimaryKey val id: String,
    val deepLink: String,
    val createdAt: Long = System.currentTimeMillis()
)
