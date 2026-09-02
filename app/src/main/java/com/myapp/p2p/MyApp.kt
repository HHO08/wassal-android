package com.myapp.p2p

import android.app.Application
import com.myapp.p2p.net.rtc.RtcManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application() {

    @Inject lateinit var rtcManager: RtcManager

    override fun onCreate() {
        super.onCreate()
        // Init native WebRTC on a background thread early.
        rtcManager.initializeAsync()
    }
}
