package com.wassal.app

import android.app.Application
import com.wassal.app.net.rtc.RtcManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WassalApp : Application() {

    @Inject lateinit var rtcManager: RtcManager

    override fun onCreate() {
        super.onCreate()
        // Init native WebRTC on a background thread early.
        rtcManager.initializeAsync()
    }
}
