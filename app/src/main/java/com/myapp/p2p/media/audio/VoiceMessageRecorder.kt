package com.myapp.p2p.media.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.myapp.p2p.data.local.MessageDao
import com.myapp.p2p.data.model.ChatMessage
import com.myapp.p2p.data.model.MessageType
import com.myapp.p2p.net.rtc.RtcManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records short voice messages to AAC (container m4a) for small size with good
 * quality, then sends them to the peer over the data channel and stores them
 * locally via Room.
 */
@Singleton
class VoiceMessageRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope,
    private val rtcManager: RtcManager,
    private val messageDao: MessageDao
) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var currentRoom: String? = null
    private var myName: String = ""

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun setUserName(name: String) {
        myName = name
    }

    fun startRecording(roomId: String): Boolean {
        return try {
            val file = File(context.filesDir, "voice_${System.currentTimeMillis()}.m4a")
            currentFile = file
            currentRoom = roomId
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(48_000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            true
        } catch (t: Throwable) {
            android.util.Log.e("VoiceRec", "start failed", t)
            false
        }
    }

    /** Stop and finalize, send to peer, and persist locally. */
    fun stopRecording() {
        val file = currentFile
        val room = currentRoom
        try {
            recorder?.stop()
        } catch (t: Throwable) {
            android.util.Log.w("VoiceRec", "stop failed", t)
        }
        runCatching { recorder?.release() }
        recorder = null

        if (file != null && room != null && file.exists()) {
            ioScope.launch {
                // Send path/hint over data channel.
                rtcManager.sendData(file.absolutePath, MessageType.VOICE)
                messageDao.insert(
                    ChatMessage(
                        roomId = room,
                        sender = myName,
                        body = file.absolutePath,
                        isMine = true,
                        type = MessageType.VOICE
                    )
                )
            }
        }
        currentFile = null
        currentRoom = null
    }
}
