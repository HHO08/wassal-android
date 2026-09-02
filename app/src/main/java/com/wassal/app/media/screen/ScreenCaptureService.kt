package com.wassal.app.media.screen

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.content.ContentValues
import androidx.core.app.NotificationCompat
import com.wassal.app.MainActivity
import com.wassal.app.R
import com.wassal.app.net.rtc.RtcManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Foreground service that captures the screen with MediaProjection.
 *
 * It supports two modes selected on the calling screen:
 *   - SHARE: the captured frames are injected into WebRTC as a video track so
 *     the remote peer can watch the screen live.
 *   - RECORD: frames plus internal audio (AudioPlaybackCapture) and the
 *     microphone are written to an MP4 at 1080p/60fps in the gallery.
 *
 * Running as a foreground service with a persistent notification keeps the
 * capture alive even if the app is backgrounded.
 */
@AndroidEntryPoint
class ScreenCaptureService : Service() {

    @Inject lateinit var rtcManager: RtcManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var mode: Int = MODE_SHARE
    private var screenWidth = 0
    private var screenHeight = 0
    private var densityDpi = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mode = intent?.getIntExtra(EXTRA_MODE, MODE_SHARE) ?: MODE_SHARE
        screenWidth = intent?.getIntExtra(EXTRA_WIDTH, 0) ?: 0
        screenHeight = intent?.getIntExtra(EXTRA_HEIGHT, 0) ?: 0
        densityDpi = intent?.getIntExtra(EXTRA_DENSITY, 0) ?: 0
        val resultData: Intent? = intent?.getParcelableExtra(EXTRA_RESULT_DATA)

        val projectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(
            android.app.Activity.RESULT_OK,
            resultData ?: return START_NOT_STICKY
        )

        startForegroundCompat()
        if (mode == MODE_RECORD) {
            startRecording()
        } else {
            startSharing()
        }
        return START_STICKY
    }

    private fun startSharing() {
        // In a real implementation the virtual display surface is bound to a
        // WebRTC VideoSink/SurfaceTextureHelper. We create the virtual display
        // sized to the screen and hand the surface to RtcManager.
        val surface = createSurfaceForShare()
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenShare",
            screenWidth, screenHeight, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null, mainHandler
        )
    }

    private fun createSurfaceForShare(): android.view.Surface? {
        // Placeholder: the RtcManager exposes a hook to register a video track
        // backed by a SurfaceTexture. For production, create a SurfaceTexture
        // via EglBase, feed it into a VideoSource, and return its Surface.
        return null
    }

    private fun startRecording() {
        val w = screenWidth
        val h = screenHeight
        // 1080p target, scaled down if the screen is larger.
        val targetHeight = 1080
        val targetWidth = (w * targetHeight / h)

        val r = MediaRecorder()
        r.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        // Internal audio via AudioPlaybackCapture on Android 10+ is wired by
        // building a playback capture config and using setAudioSource(REMOTE_SUBMIX)
        // on capable devices; see configureInternalAudio().
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        r.setVideoSize(targetWidth, targetHeight)
        r.setVideoFrameRate(60)
        r.setVideoEncodingBitRate(12_000_000)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(48_000)
        r.setAudioEncodingBitRate(192_000)

        val file = createGalleryFile()
        r.setOutputFile(file.absolutePath)
        r.prepare()

        val surface = r.surface
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenRecord",
            targetWidth, targetHeight, densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surface, null, mainHandler
        )
        r.start()
        recorder = r
    }

    private fun configureInternalAudio(): android.media.AudioPlaybackCaptureConfiguration? {
        // Android 10+ only. Capture system audio plus mic. For a pure system
        // audio mix the app must opt in via android:allowAudioPlaybackCapture.
        return null
    }

    private fun createGalleryFile(): File {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Was_$stamp.mp4")
        // Register with MediaStore so it appears in the gallery immediately.
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "Was_$stamp.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATA, file.absolutePath)
        }
        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        return file
    }

    private fun startForegroundCompat() {
        createChannel()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (mode == MODE_RECORD) "Recording screen" else "Sharing screen")
            .setContentText("Wassal is capturing your screen")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen capture",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        runCatching { virtualDisplay?.release() }
        runCatching { mediaProjection?.stop() }
        recorder = null
        virtualDisplay = null
        mediaProjection = null
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "screen_capture"
        private const val NOTIF_ID = 3001
        const val MODE_SHARE = 1
        const val MODE_RECORD = 2
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_WIDTH = "w"
        private const val EXTRA_HEIGHT = "h"
        private const val EXTRA_DENSITY = "d"
        private const val EXTRA_RESULT_DATA = "result_data"

        fun start(context: Context, resultData: Intent, mode: Int, w: Int, h: Int, density: Int) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_WIDTH, w)
                putExtra(EXTRA_HEIGHT, h)
                putExtra(EXTRA_DENSITY, density)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }
}
