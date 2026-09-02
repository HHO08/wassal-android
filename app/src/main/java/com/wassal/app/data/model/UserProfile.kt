package com.wassal.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local profile. Stored only on the user's device via Room. There is no cloud
 * database anywhere in the system by design.
 */
@Entity(tableName = "profiles")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val avatarBase64: String?,
    val createdAt: Long = System.currentTimeMillis()
)
