package com.anonymoustools.app.service.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.anonymoustools.app.MainActivity
import com.anonymoustools.app.R
import com.anonymoustools.app.core.capture.CaptureManager
import com.anonymoustools.app.service.gesture.GestureAccessibilityService

/**
 * Menu 2: Dedicated Screen Recording Controller Menu.
 * Modeled after native OEM recorder bar (OxygenOS / Samsung / MIUI).
 * Features live timer, stop button, snapshot, and FLAG_SECURE so it is invisible in recordings.
 */
class RecordingControllerOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "anonymous_recording_controller"
        private const val NOTIF_ID = 2005
        var isShowing = false
            private set

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, RecordingControllerOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RecordingControllerOverlayService::class.java)
            context.stopService(intent)
        }

        fun toggle(context: Context) {
            if (isShowing) {
                stop(context)
            } else {
                start(context)
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var floatRootView: View? = null

    private val handler = Handler(Looper.getMainLooper())
    private var startTimeMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isShowing = true
        createNotificationChannel()
        startForeground(NOTIF_ID, buildSilentNotification())
        initRecorderBar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Optimization",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background optimization"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildSilentNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Service")
            .setContentText("Running in background")
            .setSmallIcon(R.drawable.ic_qs_camera)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initRecorderBar() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // FLAG_SECURE completely excludes this overlay from MediaProjection screen recordings
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 260
        }

        val density = resources.displayMetrics.density
        val dp36 = (36 * density).toInt()
        val dp6 = (6 * density).toInt()
        val dp8 = (8 * density).toInt()
        val dp12 = (12 * density).toInt()

        // Root Capsule
        val rootCapsule = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_recorder_capsule)
            setPadding(dp12, dp6, dp12, dp6)
            gravity = Gravity.CENTER_VERTICAL
            elevation = 16 * density
        }

        // Stop Button (Red/Dark stop square)
        val stopBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_rec_stop)
            layoutParams = LinearLayout.LayoutParams(dp36, dp36)
            setPadding(dp6, dp6, dp6, dp6)
        }

        // Live Digital Timer
        val timerText = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.parseColor("#111111"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setPadding(dp6, 0, dp8, 0)
        }

        // Snapshot Button
        val snapBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_qs_camera)
            layoutParams = LinearLayout.LayoutParams(dp36, dp36)
            setPadding(dp6, dp6, dp6, dp6)
            setColorFilter(Color.parseColor("#111111"))
        }

        // Vault Shortcut Button
        val vaultBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_vault_folder)
            layoutParams = LinearLayout.LayoutParams(dp36, dp36)
            setPadding(dp6, dp6, dp6, dp6)
            setColorFilter(Color.parseColor("#111111"))
        }

        // Close Button
        val closeBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_close_circle)
            layoutParams = LinearLayout.LayoutParams(dp36, dp36)
            setPadding(dp6, dp6, dp6, dp6)
            setColorFilter(Color.parseColor("#111111"))
        }

        // Timer update runnable
        startTimeMs = SystemClock.elapsedRealtime()
        val timerRunnable = object : Runnable {
            override fun run() {
                val elapsedSec = (SystemClock.elapsedRealtime() - startTimeMs) / 1000
                val minutes = elapsedSec / 60
                val seconds = elapsedSec % 60
                timerText.text = String.format("%02d:%02d", minutes, seconds)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable)

        // STOP BUTTON CLICK:
        // Stops recording, saves to vault, and immediately closes Menu 2
        stopBtn.setOnClickListener {
            if (CaptureManager.isRecording) {
                CaptureManager.stopScreenRecording()
                Toast.makeText(applicationContext, "Recording stopped & saved to Vault", Toast.LENGTH_SHORT).show()
            }
            stopSelf()
        }

        // SNAPSHOT BUTTON CLICK:
        snapBtn.setOnClickListener {
            val acc = GestureAccessibilityService.instance
            if (acc != null) {
                rootCapsule.visibility = View.GONE
                handler.postDelayed({
                    acc.takeSilentScreenshot { _ ->
                        rootCapsule.visibility = View.VISIBLE
                    }
                }, 100)
            } else {
                Toast.makeText(applicationContext, "Enable Accessibility in Anonymous-Tools first!", Toast.LENGTH_SHORT).show()
            }
        }

        // VAULT BUTTON CLICK:
        vaultBtn.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("target_screen", "vault")
            }
            startActivity(intent)
        }

        // CLOSE BUTTON CLICK:
        closeBtn.setOnClickListener {
            if (CaptureManager.isRecording) {
                CaptureManager.stopScreenRecording()
                Toast.makeText(applicationContext, "Recording stopped & saved to Vault", Toast.LENGTH_SHORT).show()
            }
            stopSelf()
        }

        rootCapsule.addView(stopBtn)
        rootCapsule.addView(timerText)
        rootCapsule.addView(snapBtn)
        rootCapsule.addView(vaultBtn)
        rootCapsule.addView(closeBtn)

        // Smooth Drag Handling
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        rootCapsule.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        params.x = startX + dx
                        params.y = startY + dy
                        windowManager?.updateViewLayout(rootCapsule, params)
                    }
                    false
                }
                else -> false
            }
        }

        floatRootView = rootCapsule
        try {
            windowManager?.addView(rootCapsule, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
        handler.removeCallbacksAndMessages(null)
        if (floatRootView != null) {
            try {
                windowManager?.removeView(floatRootView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            floatRootView = null
        }
    }
}
