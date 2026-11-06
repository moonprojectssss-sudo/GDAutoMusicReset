package com.gdautomusic

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class DetectService : Service() {
    private var projection: MediaProjection? = null
    private var vd: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var thread: HandlerThread? = null
    private val det = Detector()
    private var last = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra("code", 0) ?: 0
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>("data")
        startFg()
        if (data == null) {
            Bus.error = "No screen capture permission data"
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            begin(code, data)
            Bus.error = null
        } catch (e: Exception) {
            Bus.error = "Capture failed: ${e.message}"
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("detect", "Screen detection", NotificationManager.IMPORTANCE_LOW)
        )
        val n = NotificationCompat.Builder(this, "detect")
            .setContentTitle("GD Auto Music Reset")
            .setContentText("Screen detection running (analyzed locally)")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(2, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(2, n)
        }
    }

    private fun begin(code: Int, data: Intent) {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = mgr.getMediaProjection(code, data)
        projection = p
        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }, Handler(Looper.getMainLooper()))

        val dm = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(dm)
        val w = dm.widthPixels / 3
        val h = dm.heightPixels / 3

        val t = HandlerThread("detect")
        t.start()
        thread = t
        val hd = Handler(t.looper)

        val r = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        reader = r
        r.setOnImageAvailableListener({ rd ->
            val img = rd.acquireLatestImage()
            if (img != null) {
                try {
                    val now = SystemClock.elapsedRealtime()
                    if (now - last >= 100) {
                        last = now
                        val pl = img.planes[0]
                        det.feed(pl.buffer, img.width, img.height, pl.rowStride, now)
                    }
                } finally {
                    img.close()
                }
            }
        }, hd)

        vd = p.createVirtualDisplay(
            "gdreset", w, h, dm.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, r.surface, null, hd
        )
        Bus.resets = 0
        Bus.state = "EDITOR"
        Bus.running = true
    }

    override fun onDestroy() {
        try { vd?.release() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        try { projection?.stop() } catch (_: Exception) {}
        thread?.quitSafely()
        Bus.running = false
        Bus.state = "OFF"
        super.onDestroy()
    }
}
