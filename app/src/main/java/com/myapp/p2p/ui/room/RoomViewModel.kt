package com.myapp.p2p.ui.room

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.p2p.data.local.ProfileRepository
import com.myapp.p2p.data.local.RoomDao
import com.myapp.p2p.media.audio.CallService
import com.myapp.p2p.media.audio.VoiceMessageRecorder
import com.myapp.p2p.net.rtc.RtcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pre-entry waiting screen for a room. Shows a quick toggle to lock or unlock
 * the microphone before actually joining, then starts the call service and the
 * WebRTC connection.
 */
@HiltViewModel
class RoomViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rtcManager: RtcManager,
    private val roomDao: RoomDao,
    private val profileRepository: ProfileRepository,
    private val voiceMessageRecorder: VoiceMessageRecorder
) : ViewModel() {

    val micLocked = mutableStateOf(false)

    fun joinRoom(roomId: String, isHost: Boolean) {
        val micEnabled = !micLocked.value
        rtcManager.setMicEnabled(micEnabled)
        CallService.start(context, micEnabled)

        rtcManager.joinRoom(roomId) {
            // Host callback: peer present, offer starts automatically.
        }
    }

    fun toggleMic() {
        micLocked.value = !micLocked.value
    }

    fun endCall() {
        CallService.stop(context)
        rtcManager.dispose()
    }
}
