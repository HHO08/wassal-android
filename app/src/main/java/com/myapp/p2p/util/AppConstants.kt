package com.myapp.p2p.util

/** Central place for network endpoints and deep-link scheme. */
object AppConstants {
    /** WebSocket signalling relay (Cloudflare Worker with Durable Object). */
    const val SIGNALING_URL = "wss://myapp-signaling.mouadhbelwassa39.workers.dev/ws"

    /** Public Google STUN servers used to establish direct P2P connectivity. */
    val STUN_SERVERS = listOf(
        "stun:stun.l.google.com:19302",
        "stun:stun1.l.google.com:19302",
        "stun:stun2.l.google.com:19302",
        "stun:stun3.l.google.com:19302",
        "stun:stun4.l.google.com:19302"
    )

    /** Deep-link scheme, e.g. myapp://room/{ROOM_ID}. */
    const val DEEP_LINK_SCHEME = "myapp"
    const val DEEP_LINK_HOST = "room"
}
