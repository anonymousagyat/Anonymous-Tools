package com.anonymoustools.app.service.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.anonymoustools.app.R
import com.anonymoustools.app.core.capture.CaptureManager
import com.anonymoustools.app.core.vault.PrivateVaultManager
import com.anonymoustools.app.service.gesture.GestureAccessibilityService
import com.anonymoustools.app.service.overlay.RecordingControllerOverlayService

class CaptureService : Service() {

    companion object {
        const val CHANNEL_ID = "anonymous_sys_sync"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_PROJECTION = "ACTION_START_PROJECTION"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_SNAPSHOT = "ACTION_SNAPSHOT"

        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"

        fun startRecordingWithPermission(context: Context, resultCode: Int, resultData: Intent) {
            val intent = Intent(context, CaptureService::class.java).apply {
                action = ACTION_START_PROJECTION
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CaptureService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createStealthNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createStealthNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        when (intent?.action) {
            ACTION_START_PROJECTION -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                if (resultCode != 0 && resultData != null) {
                    try {
                        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                        val projection = mpm.getMediaProjection(resultCode, resultData)
                        CaptureManager.activeProjection = projection

                        // Mandatory callback on Android 14
                        projection.registerCallback(object : MediaProjection.Callback() {
                            override fun onStop() {
                                CaptureManager.stopScreenRecording()
                                RecordingControllerOverlayService.stop(this@CaptureService)
                            }
                        }, Handler(Looper.getMainLooper()))

                        val outputFile = PrivateVaultManager.createVideoOutputFile(this)
                        val started = CaptureManager.startScreenRecording(this, outputFile)
                        if (started) {
                            // Pure Stealth: Double-vibration confirmation; NO overlay on screen to prevent black boxes in video
                            GestureAccessibilityService.instance?.performDoubleVibration()
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(applicationContext, "Recording started (Tap tile to stop)", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(applicationContext, "Failed to start recording encoder", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(applicationContext, "Error starting capture: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            ACTION_SNAPSHOT -> {
                CaptureManager.takeSilentSnapshot(this) { _ -> }
            }
            ACTION_STOP -> {
                if (CaptureManager.isRecording) {
                    CaptureManager.stopScreenRecording()
                }
                RecordingControllerOverlayService.stop(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createStealthNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background synchronization"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createStealthNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Service")
            .setContentText("Background process active")
            .setSmallIcon(R.drawable.ic_qs_camera)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }
}
