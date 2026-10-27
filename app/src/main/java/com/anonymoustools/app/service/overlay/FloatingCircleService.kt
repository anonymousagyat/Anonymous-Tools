package com.anonymoustools.app.service.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Backward compatibility wrapper for RecordingControllerOverlayService.
 */
class FloatingCircleService : Service() {

    companion object {
        val isShowing: Boolean
            get() = RecordingControllerOverlayService.isShowing

        fun start(context: Context) = RecordingControllerOverlayService.start(context)
        fun show(context: Context) = RecordingControllerOverlayService.start(context)
        fun stop(context: Context) = RecordingControllerOverlayService.stop(context)
        fun hide(context: Context) = RecordingControllerOverlayService.stop(context)
        fun toggle(context: Context) = RecordingControllerOverlayService.toggle(context)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        RecordingControllerOverlayService.start(this)
        stopSelf()
    }
}
