package com.wassal.app.ui.chat

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wassal.app.data.local.MessageDao
import com.wassal.app.data.local.ProfileRepository
import com.wassal.app.data.model.ChatMessage
import com.wassal.app.data.model.MessageType
import com.wassal.app.media.audio.VoiceMessageRecorder
import com.wassal.app.net.rtc.RtcManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageDao: MessageDao,
    private val rtcManager: RtcManager,
    private val profileRepository: ProfileRepository,
    private val voiceMessageRecorder: VoiceMessageRecorder
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var currentRoom: String = ""
    private var myName: String = "Me"

    init {
        viewModelScope.launch {
            myName = profileRepository.getProfile()?.displayName ?: "Me"
            voiceMessageRecorder.setUserName(myName)
        }
        viewModelScope.launch {
            rtcManager.onData.collect { (payload, type) ->
                if (currentRoom.isNotBlank()) {
                    val msg = ChatMessage(
                        roomId = currentRoom,
                        sender = "Peer",
                        body = payload,
                        isMine = false,
                        type = type
                    )
                    messageDao.insert(msg)
                    _messages.update { it + msg }
                }
            }
        }
    }

    fun bindRoom(roomId: String) {
        currentRoom = roomId
        viewModelScope.launch {
            val existing = messageDao.observeByRoom(roomId).first()
            _messages.value = existing
        }
    }

    fun sendText(text: String) {
        if (text.isBlank()) return
        rtcManager.sendData(text, MessageType.TEXT)
        val msg = ChatMessage(
            roomId = currentRoom,
            sender = myName,
            body = text,
            isMine = true,
            type = MessageType.TEXT
        )
        viewModelScope.launch {
            messageDao.insert(msg)
            _messages.update { it + msg }
        }
    }

    fun startVoiceRecording() {
        voiceMessageRecorder.startRecording(currentRoom)
    }

    fun stopVoiceRecording() {
        voiceMessageRecorder.stopRecording()
    }
}
