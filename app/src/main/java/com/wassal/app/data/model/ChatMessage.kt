package com.wassal.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single chat message exchanged over the WebRTC DataChannel and stored locally. */
@Entity(tableName = "messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: String,
    val sender: String,
    val body: String,
    val isMine: Boolean,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageType {
    TEXT,
    VOICE, // AAC audio stored in an .m4a container, path in [body]
    LINK
}
