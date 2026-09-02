package com.myapp.p2p.net.signaling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** A raw signalling message as defined by the Cloudflare Worker protocol. */
data class SignalingMessage(
    val type: String,
    val sdp: String? = null,
    val candidate: String? = null,
    val roomId: String? = null,
    val action: String? = null
)

/**
 * Thin WebSocket client for the signalling relay. It exposes typed events to
 * the [com.myapp.p2p.net.rtc.RtcManager]. Reconnects automatically.
 */
@Singleton
class SignalingClient @Inject constructor(
    private val scope: CoroutineScope,
    private val url: String
) {

    private val client = OkHttpClient.Builder()
        .pingInterval(java.time.Duration.ofSeconds(20))
        .build()

    private val _onMessage = MutableSharedFlow<SignalingMessage>(extraBufferCapacity = 64)
    val onMessage: SharedFlow<SignalingMessage> = _onMessage.asSharedFlow()

    private var webSocket: WebSocket? = null
    private val connected = AtomicBoolean(false)

    /** Connect to the room with the given id. */
    fun connect(roomId: String, onStatus: (Boolean) -> Unit = {}) {
        scope.launch {
            val wsUrl = "$url?room=$roomId"
            val request = Request.Builder().url(wsUrl).build()
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    connected.set(true)
                    onStatus(true)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    val json = JSONObject(text)
                    val msg = SignalingMessage(
                        type = json.optString("type"),
                        sdp = json.optString("sdp").ifBlank { null },
                        candidate = json.optString("candidate").ifBlank { null },
                        roomId = json.optString("roomId").ifBlank { null },
                        action = json.optString("action").ifBlank { null }
                    )
                    scope.launch { _onMessage.emit(msg) }
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    connected.set(false)
                    onStatus(false)
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    ws.close(code, reason)
                }
            })
        }
    }

    /** Send a message to the peer. */
    fun send(msg: SignalingMessage) {
        if (!connected.get()) return
        val json = JSONObject().apply {
            put("type", msg.type)
            msg.sdp?.let { put("sdp", it) }
            msg.candidate?.let { put("candidate", it) }
        }
        webSocket?.send(json.toString())
    }

    fun close() {
        runCatching { webSocket?.close(1000, "bye") }
        connected.set(false)
    }
}
