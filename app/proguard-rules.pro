# Keep WebRTC native classes from being stripped/renamed.
-keep class org.webrtc.** { *; }

# Keep Kotlin coroutines.
-dontwarn kotlinx.coroutines.**

# Room.
-keep class com.myapp.p2p.data.local.** { *; }
