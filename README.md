# Wassal Android

Direct peer-to-peer calls with excellent voice quality and screen sharing.
No cloud storage, no central servers after the initial handshake. 100% private.

## Tech stack

- Kotlin + Jetpack Compose, MVVM with Hilt DI
- Room for local-only storage (profile, chats, voice messages, rooms)
- WebRTC for direct P2P audio, Opus @ 48 kHz with high bitrate + FEC
- WebSocket signalling relay on Cloudflare Workers (Durable Objects)
- MediaProjection for screen share and 1080p/60 screen recording
- Voice messages encoded as AAC inside an m4a container

## Permissions

Only `RECORD_AUDIO` is requested at runtime. The camera is never used or
requested anywhere in the app.

## Signalling endpoint

The relay URL lives in `util/AppConstants.kt` and points at the deployed
Cloudflare Worker. It is used only for the initial SDP/ICE exchange.

## Build

Open the project in Android Studio (latest stable) and run the `app` module,
or build from the command line:

```bash
./gradlew assembleRelease
```

The release APK is written to `app/build/outputs/apk/release/`.
