package com.wassal.app.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pre-entry screen shown before actually joining the room. Gives the user a
 * fast, clear choice to lock or unlock the microphone, then enters the call.
 */
@Composable
fun RoomScreen(
    viewModel: RoomViewModel,
    roomId: String,
    isHost: Boolean,
    onStartCall: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Join room", style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Microphone")
                Switch(
                    checked = !viewModel.micLocked.value,
                    onCheckedChange = { viewModel.toggleMic() }
                )
                Text(
                    if (viewModel.micLocked.value) "Mic locked before entering"
                    else "Mic will be open"
                )
            }
        }

        Button(
            onClick = {
                viewModel.joinRoom(roomId, isHost)
                onStartCall()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter room")
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
