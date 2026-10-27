package com.anonymoustools.app.service.gesture

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.anonymoustools.app.core.capture.CaptureManager
import com.anonymoustools.app.core.capture.FrameVideoEncoder
import com.anonymoustools.app.core.vault.PrivateVaultManager
import com.anonymoustools.app.service.overlay.RecordingControllerOverlayService
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class GestureAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AnonymousGesture"
        var instance: GestureAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "GestureAccessibilityService connected successfully")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not consuming events to preserve maximum performance
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    private var isVolumeUpPressed = false
    private var isVolumeDownPressed = false
    private var volumeUpTime = 0L
    private var volumeDownTime = 0L
    private var lastScreenshotTriggerTime = 0L

    var isSequentialRecording = false
        private set
    private var sessionFrameIndex = 0
    private var sessionStartTimeMs = 0L
    private var currentIntervalDelay = 35L
    private var currentSessionDir: File? = null
    private val captureHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun startSequentialCapture(context: Context): Boolean {
        if (isSequentialRecording) return false
        isSequentialRecording = true
        sessionFrameIndex = 0
        sessionStartTimeMs = SystemClock.elapsedRealtime()
        currentIntervalDelay = 35L
        val timestamp = System.currentTimeMillis()
        val sessionDir = File(context.cacheDir, "rec_session_$timestamp")
        sessionDir.mkdirs()
        currentSessionDir = sessionDir

        performDoubleVibration()
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "Recording started (Tap tile to stop)", Toast.LENGTH_SHORT).show()
        }

        dispatchNextFrame()
        return true
    }

    private fun dispatchNextFrame() {
        if (!isSequentialRecording) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(result: ScreenshotResult) {
                            val hardwareBuffer = result.hardwareBuffer
                            val colorSpace = result.colorSpace
                            val bitmap = try {
                                Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                    ?.copy(Bitmap.Config.ARGB_8888, false)
                            } finally {
                                hardwareBuffer.close()
                            }

                            if (bitmap != null && isSequentialRecording) {
                                val frameIndex = sessionFrameIndex++
                                val dir = currentSessionDir
                                if (dir != null && dir.exists()) {
                                    // Asynchronous IO save on background thread: 0ms blocking!
                                    serviceScope.launch(Dispatchers.IO) {
                                        val frameFile = File(dir, String.format("frame_%05d.jpg", frameIndex))
                                        try {
                                            FileOutputStream(frameFile).use { out ->
                                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        } finally {
                                            bitmap.recycle()
                                        }
                                    }
                                } else {
                                    bitmap.recycle()
                                }
                            }

                            if (isSequentialRecording) {
                                // Adaptive maximum speed: ramp down towards 25ms
                                if (currentIntervalDelay > 25L) {
                                    currentIntervalDelay -= 1L
                                }
                                captureHandler.postDelayed({ dispatchNextFrame() }, currentIntervalDelay)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "Sequential frame capture code: $errorCode")
                            if (isSequentialRecording) {
                                // If throttled by OS interval limit (code 3), back off slightly
                                if (errorCode == 3) {
                                    currentIntervalDelay = Math.min(currentIntervalDelay + 40L, 200L)
                                }
                                captureHandler.postDelayed({ dispatchNextFrame() }, currentIntervalDelay)
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in dispatchNextFrame", e)
                if (isSequentialRecording) {
                    captureHandler.postDelayed({ dispatchNextFrame() }, 200)
                }
            }
        }
    }

    fun stopSequentialCapture(onComplete: ((File?) -> Unit)? = null) {
        if (!isSequentialRecording) {
            onComplete?.invoke(null)
            return
        }
        isSequentialRecording = false
        captureHandler.removeCallbacksAndMessages(null)
        performVibration()

        val sessionDir = currentSessionDir
        val totalFrames = sessionFrameIndex
        val elapsedSec = Math.max(1L, (SystemClock.elapsedRealtime() - sessionStartTimeMs) / 1000)

        if (sessionDir == null || !sessionDir.exists()) {
            onComplete?.invoke(null)
            return
        }

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(applicationContext, "Compiling video to Vault...", Toast.LENGTH_SHORT).show()
        }

        serviceScope.launch(Dispatchers.IO) {
            // Dynamic FPS calculation matching actual real-time duration
            val calculatedFps = Math.max(2, Math.min(10, Math.round(totalFrames.toFloat() / elapsedSec)))
            val outputFile = PrivateVaultManager.createVideoOutputFile(applicationContext)
            val result = FrameVideoEncoder.encodeFramesToVideo(sessionDir, outputFile, fps = calculatedFps)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    Toast.makeText(applicationContext, "Video saved to Vault!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "Failed to compile video", Toast.LENGTH_SHORT).show()
                }
                onComplete?.invoke(result)
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false
        val keyCode = event.keyCode
        val action = event.action
        val now = SystemClock.uptimeMillis()

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    isVolumeUpPressed = true
                    volumeUpTime = now
                    if (isVolumeDownPressed && (now - volumeDownTime < 600)) {
                        triggerVolumeShortcut(now)
                        return true
                    }
                } else if (action == KeyEvent.ACTION_UP) {
                    isVolumeUpPressed = false
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    isVolumeDownPressed = true
                    volumeDownTime = now
                    if (isVolumeUpPressed && (now - volumeUpTime < 600)) {
                        triggerVolumeShortcut(now)
                        return true
                    }
                } else if (action == KeyEvent.ACTION_UP) {
                    isVolumeDownPressed = false
                }
            }
        }
        return super.onKeyEvent(event)
    }

    private fun triggerVolumeShortcut(now: Long) {
        if (now - lastScreenshotTriggerTime > 1200) {
            lastScreenshotTriggerTime = now
            if (isSequentialRecording) {
                stopSequentialCapture()
            } else if (CaptureManager.isRecording) {
                CaptureManager.stopScreenRecording()
                performVibration()
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(applicationContext, "Recording stopped & saved to Vault", Toast.LENGTH_SHORT).show()
                }
            } else {
                takeSilentScreenshot()
            }
        }
    }

    /**
     * Native Android 11+ silent screenshot.
     * ZERO casting prompt. ZERO screen-share dialog. ZERO shutter sound.
     */
    fun takeSilentScreenshot(onComplete: ((File?) -> Unit)? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    mainExecutor,
                    object : TakeScreenshotCallback {
                        override fun onSuccess(result: ScreenshotResult) {
                            val hardwareBuffer = result.hardwareBuffer
                            val colorSpace = result.colorSpace
                            val bitmap = try {
                                Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                                    ?.copy(Bitmap.Config.ARGB_8888, false)
                            } finally {
                                hardwareBuffer.close()
                            }

                            if (bitmap != null) {
                                performVibration()
                                val file = PrivateVaultManager.saveSnapshot(this@GestureAccessibilityService, bitmap)
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(applicationContext, "Snapshot saved to Vault!", Toast.LENGTH_SHORT).show()
                                    onComplete?.invoke(file)
                                }
                            } else {
                                Handler(Looper.getMainLooper()).post {
                                    Toast.makeText(applicationContext, "Screenshot buffer empty", Toast.LENGTH_SHORT).show()
                                    onComplete?.invoke(null)
                                }
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.e(TAG, "takeScreenshot failed with code: $errorCode")
                            // Fallback to system global action
                            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                            Handler(Looper.getMainLooper()).post {
                                onComplete?.invoke(null)
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception calling takeScreenshot", e)
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                Handler(Looper.getMainLooper()).post {
                    onComplete?.invoke(null)
                }
            }
        } else {
            // Android 9-10 fallback
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            Handler(Looper.getMainLooper()).post {
                onComplete?.invoke(null)
            }
        }
    }

    fun performVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(35)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun performDoubleVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 90, 70), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(longArrayOf(0, 70, 90, 70), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
