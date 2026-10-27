package com.anonymoustools.app.core.capture

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FrameVideoEncoder {
    private const val TAG = "FrameVideoEncoder"
    private const val MIME_TYPE = "video/avc"
    private const val BIT_RATE = 4_000_000 // 4 Mbps
    private const val I_FRAME_INTERVAL = 1

    suspend fun encodeFramesToVideo(
        sessionDir: File,
        outputFile: File,
        fps: Int = 2
    ): File? = withContext(Dispatchers.IO) {
        val frameFiles = sessionDir.listFiles { f -> f.extension.equals("jpg", ignoreCase = true) }
            ?.sortedBy { it.name } ?: return@withContext null

        if (frameFiles.isEmpty()) return@withContext null

        // Decode first frame to get bounds
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(frameFiles.first().absolutePath, options)
        var width = (options.outWidth / 2) * 2
        var height = (options.outHeight / 2) * 2

        val maxDim = 1920
        if (width > maxDim || height > maxDim) {
            val scale = maxDim.toFloat() / Math.max(width, height)
            width = ((width * scale).toInt() / 2) * 2
            height = ((height * scale).toInt() / 2) * 2
        }

        if (width <= 0 || height <= 0) {
            width = 720
            height = 1280
        }

        outputFile.parentFile?.mkdirs()

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            }

            codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            val frameDurationUs = 1_000_000L / fps
            var presentationTimeUs = 0L

            fun drainEncoder(endOfStream: Boolean) {
                val timeoutUs = if (endOfStream) 15000L else 0L
                while (true) {
                    val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                    if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) break else break
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            break
                        }
                        val newFormat = codec.outputFormat
                        trackIndex = muxer.addTrack(newFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outputBufferId >= 0) {
                        val encodedData = codec.getOutputBuffer(outputBufferId)
                        if (encodedData != null) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                bufferInfo.size = 0
                            }

                            if (bufferInfo.size != 0 && muxerStarted) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                bufferInfo.presentationTimeUs = presentationTimeUs
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }

                            codec.releaseOutputBuffer(outputBufferId, false)
                        }

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            val destRect = Rect(0, 0, width, height)

            for (file in frameFiles) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
                val canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    inputSurface.lockHardwareCanvas()
                } else {
                    inputSurface.lockCanvas(null)
                }
                canvas.drawBitmap(bitmap, null, destRect, null)
                inputSurface.unlockCanvasAndPost(canvas)
                bitmap.recycle()

                presentationTimeUs += frameDurationUs
                drainEncoder(false)
            }

            codec.signalEndOfInputStream()
            drainEncoder(true)

            // Cleanup session directory
            sessionDir.deleteRecursively()

            return@withContext outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error encoding frames to video", e)
            outputFile.delete()
            return@withContext null
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (ignored: Exception) {}
            try {
                if (muxerStarted) {
                    muxer?.stop()
                }
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }
}
