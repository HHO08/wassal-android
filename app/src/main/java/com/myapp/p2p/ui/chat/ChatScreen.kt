package com.myapp.p2p.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myapp.p2p.data.model.ChatMessage

/**
 * Chat panel. Shows text + voice messages exchanged over the data channel.
 * Voice messages are recorded in AAC/m4a and shown as a playable item.
 */
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState()
    var text by remember { mutableStateOf("") }
    var recording by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(vertical = 8.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Message") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {
                viewModel.sendText(text)
                text = ""
            }) {
                Text("Send")
            }
            Button(onClick = {
                if (recording) {
                    viewModel.stopVoiceRecording()
                    recording = false
                } else {
                    viewModel.startVoiceRecording()
                    recording = true
                }
            }) {
                Text(if (recording) "Stop" else "Voice")
            }
        }
    }
}

@Composable
private fun MessageBubble(msg: ChatMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = if (msg.isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Text(
            text = when (msg.type) {
                com.myapp.p2p.data.model.MessageType.VOICE -> "Voice message"
                com.myapp.p2p.data.model.MessageType.LINK -> msg.body
                else -> msg.body
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (msg.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(8.dp)
        )
    }
}
