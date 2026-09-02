package com.wassal.app.ui.call

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wassal.app.media.screen.ScreenCaptureService
import com.wassal.app.ui.chat.ChatScreen

/**
 * In-call screen: mic toggle, chat, voice messages, screen share/record, and
 * end-call. Shares the room with a [ChatScreen] driven by the chat VM.
 */
@Composable
fun CallScreen(
    viewModel: CallViewModel,
    roomId: String,
    onEnd: () -> Unit
) {
    val context = LocalContext.current
    val micOn by viewModel.micOn.collectAsState()
    val sharing by viewModel.screenSharing.collectAsState()

    val chatViewModel: com.wassal.app.ui.chat.ChatViewModel = hiltViewModel()
    LaunchedEffect(roomId) { chatViewModel.bindRoom(roomId) }

    // MediaProjection launcher for screen share/record.
    var projectionMode by remember { mutableStateOf(0) }
    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val w = context.resources.displayMetrics.widthPixels
            val h = context.resources.displayMetrics.heightPixels
            val d = context.resources.displayMetrics.densityDpi
            ScreenCaptureService.start(context, result.data!!, projectionMode, w, h, d)
            viewModel.setScreenSharing(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("In call", style = MaterialTheme.typography.headlineMedium)
        Text(if (micOn) "Microphone on" else "Muted")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.toggleMic() }) {
                Text(if (micOn) "Mute" else "Unmute")
            }
            Button(onClick = {
                projectionMode = ScreenCaptureService.MODE_SHARE
                val mpm = context.getSystemService(Activity.MEDIA_PROJECTION_SERVICE)
                        as android.media.projection.MediaProjectionManager
                projectionLauncher.launch(mpm.createScreenCaptureIntent())
            }) {
                Text(if (sharing) "Sharing" else "Share screen")
            }
            Button(onClick = {
                projectionMode = ScreenCaptureService.MODE_RECORD
                val mpm = context.getSystemService(Activity.MEDIA_PROJECTION_SERVICE)
                        as android.media.projection.MediaProjectionManager
                projectionLauncher.launch(mpm.createScreenCaptureIntent())
            }) {
                Text("Record")
            }
        }

        // Embedded chat panel (takes the remaining height).
        ChatScreen(chatViewModel)

        Button(onClick = {
            viewModel.endCall()
            onEnd()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("End call")
        }
    }
}
