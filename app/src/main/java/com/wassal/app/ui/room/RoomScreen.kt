package com.wassal.app.ui.room

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Switch
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/**
 * Pre-entry screen shown before actually joining the room. Gives the user a
 * fast, clear choice to lock or unlock the microphone, then enters the call.
 *
 * RECORD_AUDIO must be granted before we start the microphone foreground
 * service, otherwise Android 13+ throws a SecurityException and crashes.
 */
@Composable
fun RoomScreen(
    viewModel: RoomViewModel,
    roomId: String,
    isHost: Boolean,
    onStartCall: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.joinRoom(roomId, isHost)
            onStartCall()
        }
    }

    fun enterRoom() {
        val alreadyGranted =
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            viewModel.joinRoom(roomId, isHost)
            onStartCall()
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

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
            onClick = { enterRoom() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enter room")
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
