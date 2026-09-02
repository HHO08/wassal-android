package com.myapp.p2p.media.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.myapp.p2p.MainActivity
import com.myapp.p2p.R
import com.myapp.p2p.net.rtc.RtcManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that holds the active WebRTC session. Keeping the call in
 * a foreground service with a persistent notification prevents Android from
 * killing the audio pipeline when the app leaves the foreground.
 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject lateinit var rtcManager: RtcManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val micEnabled = intent?.getBooleanExtra(EXTRA_MIC, true) ?: true
        rtcManager.setMicEnabled(micEnabled)
        startForegroundCompat(micEnabled)
        return START_STICKY
    }

    private fun startForegroundCompat(micEnabled: Boolean) {
        createChannel()
        val notification = buildNotification(micEnabled)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(micEnabled: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MyApp Call")
            .setContentText(if (micEnabled) "Active · direct P2P" else "Muted")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Active calls",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        rtcManager.dispose()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "calls"
        private const val NOTIF_ID = 2001
        private const val EXTRA_MIC = "mic"

        fun start(context: Context, micEnabled: Boolean) {
            val intent = Intent(context, CallService::class.java).apply {
                putExtra(EXTRA_MIC, micEnabled)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallService::class.java))
        }
    }
}
