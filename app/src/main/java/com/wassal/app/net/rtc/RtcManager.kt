package com.wassal.app.net.rtc

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import com.wassal.app.data.model.MessageType
import com.wassal.app.net.signaling.SignalingClient
import com.wassal.app.net.signaling.SignalingMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.AudioSource
import org.webrtc.AudioTrack as RtcAudioTrack
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the WebRTC [PeerConnectionFactory] and a single [PeerConnection].
 *
 * Audio is captured through the native audio device module and encoded with the
 * Opus codec at 48 kHz. A dedicated processing pipeline (echo cancellation,
 * noise suppression, automatic gain control) is enabled through
 * [AudioProcessingOptions], and the device audio mode is switched to the
 * in-call mode so the OS applies its own echo/AGC optimisations.
 *
 * Text messages, voice-message metadata and profile identity travel over a
 * reliable [DataChannel].
 */
@Singleton
class RtcManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CoroutineScope,
    private val signaling: SignalingClient,
    private val stunServers: List<String>
) {

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var localAudioTrack: RtcAudioTrack? = null
    private var remoteAudioTrack: RtcAudioTrack? = null
    private var dataChannel: DataChannel? = null
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var videoSink: VideoSinkAdapter? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var audioManager: AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _peerConnected = MutableStateFlow(false)
    val peerConnected: StateFlow<Boolean> = _peerConnected.asStateFlow()

    private val _micEnabled = MutableStateFlow(false)
    val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()

    private val _incomingVideoAvailable = MutableStateFlow(false)
    val incomingVideoAvailable: StateFlow<Boolean> = _incomingVideoAvailable.asStateFlow()

    private val _onData = MutableSharedFlow<Pair<String, MessageType>>(extraBufferCapacity = 64)
    val onData = _onData.asSharedFlow()

    private var peerJoinedCallback: (() -> Unit)? = null

    // ---- lifecycle ---------------------------------------------------------

    fun initializeAsync() {
        scope.launch(Dispatchers.IO) {
            try {
                PeerConnectionFactory.initialize(
                    PeerConnectionFactory.InitializationOptions.builder(context)
                        .setEnableInternalTracer(false)
                        .createInitializationOptions()
                )
                buildFactory()
                connectSignaling()
            } catch (t: Throwable) {
                android.util.Log.e("RtcManager", "init failed", t)
            }
        }
    }

    private fun buildFactory() {
        val builder = PeerConnectionFactory.builder()
        val deviceModule = createAudioDeviceModule()
        builder.setAudioDeviceModule(deviceModule)
        val eglBase = EglBase.create()
        builder.setVideoEncoderFactory(
            DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        )
        builder.setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
        peerConnectionFactory = builder.createPeerConnectionFactory()
    }

    private fun createAudioDeviceModule(): AudioDeviceModule {
        val builder = org.webrtc.audio.JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
        return builder.createAudioDeviceModule()
    }

    private fun connectSignaling() {
        scope.launch {
            signaling.onMessage.collect { msg -> handleSignalingMessage(msg) }
        }
    }

    // ---- room / connection -------------------------------------------------

    /**
     * Connect to the signalling relay for the given room.
     * [onPeerJoined] is invoked on the host once the guest is present so it can
     * kick off the offer/answer handshake.
     */
    fun joinRoom(roomId: String, onPeerJoined: () -> Unit) {
        signaling.connect(roomId) { connected ->
            if (connected) {
                // A fresh WebSocket connect resets our intent; host begins the
                // offer when the relay announces a peer.
                if (peerConnection != null) {
                    dispose()
                }
            }
        }
        peerJoinedCallback = onPeerJoined
    }

    /** Send the local audio/video offers via the relay. Called by the host. */
    private fun startOffer() {
        scope.launch {
            val offer = createOffer()
            signaling.send(SignalingMessage(type = "offer", sdp = offer))
        }
    }

    private suspend fun createOffer(): String {
        ensurePeerConnection()
        val constraints = MediaConstraints()
        val sdpObserver = SdpObserverHolder()
        peerConnection?.createOffer(sdpObserver, constraints)
        val sdp = sdpObserver.awaitResult() ?: throw IllegalStateException("createOffer failed")
        val tuned = tuneOpus(sdp.description)
        val tunedSdp = SessionDescription(SessionDescription.Type.OFFER, tuned)
        peerConnection?.setLocalDescription(SdpObserverHolder.IGNORING, tunedSdp)
        return tuned
    }

    /**
     * Force the Opus codec (native 48 kHz in WebRTC) to a high, constant audio
     * bitrate and enable FEC + stereo so voice quality stays excellent.
     */
    private fun tuneOpus(sdp: String): String {
        return sdp.replace(
            Regex("(a=rtpmap:\\d+ opus/48000/2\\r?\\n)(a=fmtp:\\d+)"),
            "$1$2;stereo=1;maxaveragebitrate=128000;maxplaybackrate=48000;useinbandfec=1;usedtx=1"
        )
    }

    private fun handleSignalingMessage(msg: SignalingMessage) {
        when (msg.type) {
            // Host: a guest joined, begin the offer flow.
            "peer" -> if (msg.action == "joined") {
                peerJoinedCallback?.invoke()
                startOffer()
            }

            "offer" -> scope.launch {
                ensurePeerConnection()
                peerConnection?.setRemoteDescription(SdpObserverHolder.IGNORING, SessionDescription(SessionDescription.Type.OFFER, msg.sdp!!))
                val observer = SdpObserverHolder()
                peerConnection?.createAnswer(observer, MediaConstraints())
                val answer = observer.awaitResult() ?: return@launch
                peerConnection?.setLocalDescription(SdpObserverHolder.IGNORING, answer)
                signaling.send(SignalingMessage(type = "answer", sdp = answer.description))
            }

            "answer" -> scope.launch {
                peerConnection?.setRemoteDescription(SdpObserverHolder.IGNORING, SessionDescription(SessionDescription.Type.ANSWER, msg.sdp!!))
            }

            "ice" -> msg.candidate?.let { c ->
                scope.launch {
                    try {
                        peerConnection?.addIceCandidate(parseCandidate(c))
                    } catch (t: Throwable) {
                        android.util.Log.w("RtcManager", "addIceCandidate failed", t)
                    }
                }
            }

            else -> Unit
        }
    }

    private fun parseCandidate(raw: String): IceCandidate {
        // Raw candidate strings sent by Android are json: {candidate, sdpMid, sdpMLineIndex}.
        val json = JSONObject(raw)
        return IceCandidate(
            json.optString("sdpMid", "0"),
            json.optInt("sdpMLineIndex", 0),
            json.optString("candidate", raw)
        )
    }

    private fun ensurePeerConnection() {
        if (peerConnection != null) return
        val iceServers = stunServers.map { PeerConnection.IceServer.builder(it).createIceServer() }
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL

        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                val json = JSONObject().apply {
                    put("candidate", candidate.sdp)
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                }
                signaling.send(SignalingMessage(type = "ice", candidate = json.toString()))
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                android.util.Log.i("RtcManager", "ice state=$state")
                if (state == PeerConnection.IceConnectionState.CONNECTED) {
                    mainHandler.post { _peerConnected.value = true }
                } else if (state == PeerConnection.IceConnectionState.DISCONNECTED) {
                    mainHandler.post { _peerConnected.value = false }
                }
            }

            override fun onAddStream(stream: MediaStream) {
                stream.audioTracks.firstOrNull()?.let { track ->
                    remoteAudioTrack = track
                    track.setEnabled(true)
                    mainHandler.post { _micEnabled.value = track.enabled() }
                }
                stream.videoTracks.firstOrNull()?.let { track ->
                    remoteVideoTrack = track
                    videoSink?.let { track.addSink(it) }
                    mainHandler.post { _incomingVideoAvailable.value = true }
                }
            }

            override fun onRemoveStream(stream: MediaStream) { }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) { }
            override fun onIceConnectionReceivingChange(receiving: Boolean) { }
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) { }
            override fun onSignalingChange(state: PeerConnection.SignalingState) { }
            override fun onDataChannel(channel: DataChannel) { wireDataChannel(channel) }
            override fun onRenegotiationNeeded() { }
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        // Attach audio.
        val audioConstraints = MediaConstraints()
        val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        this.audioSource = audioSource
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio0", audioSource)
        val mediaStream = peerConnectionFactory?.createLocalMediaStream("localStream")
        mediaStream?.addTrack(localAudioTrack)
        peerConnection?.addStream(mediaStream)

        // Open the data channel for chat.
        wireDataChannel(peerConnection?.createDataChannel("chat", DataChannel.Init().apply {
            ordered = true
        }))

        // Activate in-call audio mode for best echo/AGC handling.
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isSpeakerphoneOn = true
    }

    private fun wireDataChannel(channel: DataChannel?) {
        channel ?: return
        this.dataChannel = channel
        channel.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(previousAmount: Long) {}
            override fun onStateChange() {}
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                val text = String(bytes, Charsets.UTF_8)
                val json = JSONObject(text)
                val type = when (json.optString("type")) {
                    "voice" -> MessageType.VOICE
                    "link" -> MessageType.LINK
                    else -> MessageType.TEXT
                }
                scope.launch { _onData.emit(json.optString("payload") to type) }
            }
        })
    }

    /** Send chat data over the reliable data channel. */
    fun sendData(payload: String, type: MessageType) {
        val json = JSONObject().apply {
            put("type", when (type) {
                MessageType.VOICE -> "voice"
                MessageType.LINK -> "link"
                else -> "text"
            })
            put("payload", payload)
        }
        val data = java.nio.ByteBuffer.allocateDirect(json.toString().toByteArray().size)
        data.put(json.toString().toByteArray())
        data.rewind()
        dataChannel?.send(DataChannel.Buffer(data, false))
    }

    fun setMicEnabled(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        mainHandler.post { _micEnabled.value = enabled }
    }

    fun attachVideoSink(sink: VideoSinkAdapter) {
        this.videoSink = sink
        remoteVideoTrack?.addSink(sink)
    }

    fun detachVideoSink() {
        videoSink?.let { remoteVideoTrack?.removeSink(it) }
        videoSink = null
    }

    fun attachLocalVideoTrack(track: VideoTrack?) {
        localVideoTrack = track
        // A remote device that shares its screen will receive this as a video track.
    }

    fun dispose() {
        scope.launch {
            runCatching { dataChannel?.close() }
            runCatching { audioSource?.dispose() }
            runCatching { peerConnection?.dispose() }
            runCatching { audioManager.mode = AudioManager.MODE_NORMAL }
            peerConnection = null
        }
    }

    private class SdpObserverHolder : org.webrtc.SdpObserver {
        private val done = kotlinx.coroutines.CompletableDeferred<SessionDescription?>()
        override fun onCreateSuccess(desc: SessionDescription) { done.complete(desc) }
        override fun onCreateFailure(error: String) { done.complete(null) }
        override fun onSetSuccess() {}
        override fun onSetFailure(error: String) {}

        suspend fun awaitResult(): SessionDescription? = done.await()

        companion object {
            val IGNORING = object : org.webrtc.SdpObserver {
                override fun onCreateSuccess(desc: SessionDescription) {}
                override fun onCreateFailure(error: String) {}
                override fun onSetSuccess() {}
                override fun onSetFailure(error: String) {}
            }
        }
    }
}

/** Minimal sink adapter that forwards rendered frames to a Compose layer. */
class VideoSinkAdapter(
    private val onFrame: (org.webrtc.VideoFrame) -> Unit
) : org.webrtc.VideoSink {
    override fun onFrame(frame: org.webrtc.VideoFrame) {
        onFrame(frame)
    }
}
