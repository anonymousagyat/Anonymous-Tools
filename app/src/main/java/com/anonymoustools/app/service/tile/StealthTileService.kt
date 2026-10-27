package com.anonymoustools.app.service.tile

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.anonymoustools.app.core.capture.CaptureManager
import com.anonymoustools.app.service.capture.CaptureService
import com.anonymoustools.app.service.gesture.GestureAccessibilityService
import com.anonymoustools.app.service.overlay.TileMenuOverlayService

class StealthTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRec = CaptureManager.isRecording || (GestureAccessibilityService.instance?.isSequentialRecording == true)
        if (isRec) {
            tile.label = "Stop Recording"
            tile.state = Tile.STATE_ACTIVE
        } else {
            tile.label = "Anonymous Capture"
            tile.state = Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()

        // 1. If sequential capture is active: stop and compile to video!
        val acc = GestureAccessibilityService.instance
        if (acc != null && acc.isSequentialRecording) {
            acc.stopSequentialCapture()
            updateTileState()
            return
        }

        // 2. If MediaProjection recording is active: stop it!
        if (CaptureManager.isRecording) {
            CaptureManager.stopScreenRecording()
            CaptureService.stopService(this)
            GestureAccessibilityService.instance?.performVibration()
            Toast.makeText(this, "Recording stopped & saved to Vault", Toast.LENGTH_SHORT).show()
            updateTileState()
            return
        }

        if (Settings.canDrawOverlays(this)) {
            // Spawn or toggle Menu 1: The Tile Menu!
            TileMenuOverlayService.toggle(this)
        } else {
            // Direct user to grant "Display over other apps" permission
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }
}
