package com.myapp.p2p.ui.call

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.p2p.data.local.ProfileRepository
import com.myapp.p2p.media.audio.CallService
import com.myapp.p2p.media.screen.ScreenCaptureService
import com.myapp.p2p.net.rtc.RtcManager
import com.myapp.p2p.ui.chat.ChatViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rtcManager: RtcManager
) : ViewModel() {

    private val _micOn = MutableStateFlow(true)
    val micOn: StateFlow<Boolean> = _micOn.asStateFlow()

    private val _screenSharing = MutableStateFlow(false)
    val screenSharing: StateFlow<Boolean> = _screenSharing.asStateFlow()

    fun toggleMic() {
        val next = !_micOn.value
        rtcManager.setMicEnabled(next)
        _micOn.value = next
    }

    fun setScreenSharing(on: Boolean) {
        _screenSharing.value = on
    }

    fun endCall() {
        CallService.stop(context)
        rtcManager.dispose()
    }
}
