package com.anonymoustools.app.core.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import com.anonymoustools.app.core.vault.PrivateVaultManager
import java.io.File

object CaptureManager {

    var activeProjection: MediaProjection? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingVirtualDisplay: VirtualDisplay? = null
    var isRecording = false
        private set

    fun getScreenMetrics(context: Context): DisplayMetrics {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = context.resources.configuration.densityDpi
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
        }
        return metrics
    }

    fun takeSilentSnapshot(context: Context, onComplete: (File?) -> Unit) {
        val projection = activeProjection
        if (projection == null) {
            onComplete(null)
            return
        }

        val metrics = getScreenMetrics(context)
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        var virtualDisplay: VirtualDisplay? = null

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                try {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * width

                    val bitmap = Bitmap.createBitmap(
                        width + rowPadding / pixelStride,
                        height,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)
                    val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)

                    val savedFile = PrivateVaultManager.saveSnapshot(context, croppedBitmap)
                    onComplete(savedFile)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onComplete(null)
                } finally {
                    image.close()
                    virtualDisplay?.release()
                    imageReader.close()
                }
            }
        }, Handler(Looper.getMainLooper()))

        virtualDisplay = projection.createVirtualDisplay(
            "AnonymousSilentSnapshot",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            null
        )
    }

    fun startScreenRecording(context: Context, outputFile: File): Boolean {
        val projection = activeProjection ?: return false
        if (isRecording) return false

        val metrics = getScreenMetrics(context)
        val rawWidth = metrics.widthPixels
        val rawHeight = metrics.heightPixels
        val dpi = metrics.densityDpi

        outputFile.parentFile?.mkdirs()

        // Attempt 1: Native screen resolution (even dimensions)
        val nativeW = (rawWidth / 2) * 2
        val nativeH = (rawHeight / 2) * 2

        var started = attemptStartRecorder(context, projection, outputFile, nativeW, nativeH, dpi)
        if (!started) {
            // Attempt 2: Universal 720p proportional fallback (supported on all phone hardware)
            val fallbackW = 720
            val fallbackH = (((rawHeight.toFloat() / rawWidth) * 720).toInt() / 2) * 2
            started = attemptStartRecorder(context, projection, outputFile, fallbackW, fallbackH, dpi)
        }
        return started
    }

    private fun attemptStartRecorder(
        context: Context,
        projection: MediaProjection,
        outputFile: File,
        width: Int,
        height: Int,
        dpi: Int
    ): Boolean {
        return try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.setVideoSize(width, height)
            recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            recorder.setVideoEncodingBitRate(5 * 1000 * 1000)
            recorder.setVideoFrameRate(30)
            recorder.prepare()

            val surface = recorder.surface
            recordingVirtualDisplay = projection.createVirtualDisplay(
                "AnonymousScreenRecorder",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface,
                null,
                null
            )

            recorder.start()
            mediaRecorder = recorder
            isRecording = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                mediaRecorder?.reset()
                mediaRecorder?.release()
            } catch (ignored: Exception) {}
            mediaRecorder = null
            recordingVirtualDisplay?.release()
            recordingVirtualDisplay = null
            isRecording = false
            false
        }
    }

    fun stopScreenRecording(): Boolean {
        if (!isRecording) return false
        return try {
            try {
                mediaRecorder?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                mediaRecorder?.reset()
                mediaRecorder?.release()
            } catch (ignored: Exception) {}
            mediaRecorder = null

            recordingVirtualDisplay?.release()
            recordingVirtualDisplay = null
            isRecording = false
            true
        } catch (e: Exception) {
            e.printStackTrace()
            mediaRecorder = null
            recordingVirtualDisplay = null
            isRecording = false
            false
        }
    }
}
