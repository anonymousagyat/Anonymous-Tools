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
import android.os.IBinder
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
import com.anonymoustools.app.service.capture.RecordPermissionActivity
import com.anonymoustools.app.service.gesture.GestureAccessibilityService

/**
 * Menu 1: Floating Tile Menu (Options popup).
 * Triggered when the user taps the Quick Settings Tile from the Android notification shade.
 * Displays quick options: Snapshot, Screen Recording, Vault, Close.
 * Visually distinct from Menu 2 (The Recording Controller Bar).
 */
class TileMenuOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "anonymous_tile_menu"
        private const val NOTIF_ID = 2006
        var isShowing = false
            private set

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, TileMenuOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, TileMenuOverlayService::class.java)
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isShowing = true
        createNotificationChannel()
        startForeground(NOTIF_ID, buildSilentNotification())
        initTileMenu()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Utilities",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Background utilities"
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
            .setContentText("Utility tools active")
            .setSmallIcon(R.drawable.ic_qs_camera)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun initTileMenu() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = -100
        }

        val density = resources.displayMetrics.density
        val dp4 = (4 * density).toInt()
        val dp8 = (8 * density).toInt()
        val dp12 = (12 * density).toInt()
        val dp16 = (16 * density).toInt()
        val dp24 = (24 * density).toInt()
        val dp48 = (48 * density).toInt()

        // Root Dialog Container (Menu 1 - Distinct Box Card)
        val rootCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_floating_panel)
            setPadding(dp16, dp12, dp16, dp16)
            elevation = 20 * density
        }

        // Top Header Row
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleView = TextView(this).apply {
            text = "ANONYMOUS TOOLS"
            setTextColor(Color.parseColor("#111111"))
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = ImageView(this).apply {
            setImageResource(R.drawable.ic_close_circle)
            layoutParams = LinearLayout.LayoutParams(dp24, dp24)
            setColorFilter(Color.parseColor("#111111"))
            setOnClickListener { stopSelf() }
        }

        headerRow.addView(titleView)
        headerRow.addView(closeBtn)
        rootCard.addView(headerRow)

        // Actions Row (Snapshot, Recording, Vault)
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp12, 0, 0)
        }

        fun createActionButton(iconRes: Int, labelText: String, onClick: () -> Unit): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp8, dp4, dp8, dp4)
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }

                val iconView = ImageView(context).apply {
                    setImageResource(iconRes)
                    layoutParams = LinearLayout.LayoutParams(dp48, dp48).apply {
                        setMargins(dp4, dp4, dp4, dp4)
                    }
                    setPadding(dp8, dp8, dp8, dp8)
                    setBackgroundResource(R.drawable.bg_floating_bubble)
                    setColorFilter(Color.parseColor("#111111"))
                }

                val textView = TextView(context).apply {
                    text = labelText
                    textSize = 11f
                    setTextColor(Color.parseColor("#111111"))
                    typeface = Typeface.DEFAULT_BOLD
                }

                addView(iconView)
                addView(textView)
            }
        }

        // 1. Silent Snapshot Action
        val snapAction = createActionButton(R.drawable.ic_qs_camera, "Snapshot") {
            val acc = GestureAccessibilityService.instance
            if (acc != null) {
                rootCard.visibility = View.GONE
                acc.takeSilentScreenshot { _ ->
                    stopSelf()
                }
            } else {
                Toast.makeText(applicationContext, "Enable Accessibility in Anonymous-Tools first!", Toast.LENGTH_SHORT).show()
                stopSelf()
            }
        }

        // 2. Sequential Screen Recording Action (Multiple screenshots compiled to video)
        val recordAction = createActionButton(R.drawable.ic_video_record, "Record") {
            stopSelf()
            val acc = GestureAccessibilityService.instance
            if (acc != null) {
                if (acc.isSequentialRecording) {
                    acc.stopSequentialCapture()
                } else {
                    acc.startSequentialCapture(applicationContext)
                }
            } else {
                Toast.makeText(applicationContext, "Enable Accessibility in Anonymous-Tools first!", Toast.LENGTH_SHORT).show()
            }
        }

        // 3. Vault Action
        val vaultAction = createActionButton(R.drawable.ic_vault_folder, "Vault") {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("target_screen", "vault")
            }
            startActivity(intent)
            stopSelf()
        }

        actionsRow.addView(snapAction)
        actionsRow.addView(recordAction)
        actionsRow.addView(vaultAction)

        rootCard.addView(actionsRow)

        // Smooth Drag Handling
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        rootCard.setOnTouchListener { _, event ->
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
                        windowManager?.updateViewLayout(rootCard, params)
                    }
                    false
                }
                else -> false
            }
        }

        floatRootView = rootCard
        try {
            windowManager?.addView(rootCard, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing = false
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
