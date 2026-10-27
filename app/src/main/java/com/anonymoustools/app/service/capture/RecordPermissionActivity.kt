package com.anonymoustools.app.service.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.anonymoustools.app.core.capture.CaptureManager
import com.anonymoustools.app.core.vault.PrivateVaultManager
import com.anonymoustools.app.service.overlay.RecordingControllerOverlayService

class RecordPermissionActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, RecordPermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(intent)
        }
    }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Route through Foreground Service to strictly satisfy Android 14 requirements
            CaptureService.startRecordingWithPermission(this, result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screen recording cancelled", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (CaptureManager.activeProjection != null && !CaptureManager.isRecording) {
            val outputFile = PrivateVaultManager.createVideoOutputFile(this)
            val started = CaptureManager.startScreenRecording(this, outputFile)
            if (started) {
                com.anonymoustools.app.service.gesture.GestureAccessibilityService.instance?.performDoubleVibration()
                Toast.makeText(this, "Recording started (Tap tile to stop)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
            }
            finish()
        } else {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projectionLauncher.launch(mpm.createScreenCaptureIntent())
        }
    }
}
