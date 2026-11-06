package com.gdautomusic

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val GD_PACKAGE = "com.robtopx.geometryjump"
    }

    var svc: MusicService? = null
    val ui = Handler(Looper.getMainLooper())
    val prefs by lazy { getSharedPreferences("gd", Context.MODE_PRIVATE) }
    var pendingLaunch = false
    var dragging = false

    lateinit var trackTitle: TextView
    lateinit var timeText: TextView
    lateinit var seek: SeekBar
    lateinit var playBtn: Button
    lateinit var detVal: TextView
    lateinit var stateVal: TextView
    lateinit var autoVal: TextView
    lateinit var resetVal: TextView
    lateinit var errText: TextView
    lateinit var detBtn: Button

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            val s = (b as MusicService.LocalBinder).service
            svc = s
            Bus.music = s
            s.onError = { msg -> runOnUiThread { toast(msg) } }
            s.player.volume = prefs.getInt("vol", 100) / 100f
            s.setLoop(prefs.getBoolean("loop", true))
            if (s.player.mediaItemCount == 0) {
                prefs.getString("uri", null)?.let { load(Uri.parse(it)) }
            }
        }

        override fun onServiceDisconnected(n: ComponentName) {
            svc = null
            Bus.music = null
        }
    }

    val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        prefs.edit().putString("uri", uri.toString()).apply()
        load(uri)
    }

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val capture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val data = r.data
        if (r.resultCode == RESULT_OK && data != null) {
            val i = Intent(this, DetectService::class.java)
            i.putExtra("code", r.resultCode)
            i.putExtra("data", data)
            ContextCompat.startForegroundService(this, i)
        } else {
            toast("Screen capture permission denied. Detection is off.")
        }
        if (pendingLaunch) {
            pendingLaunch = false
            launchGd()
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            svc?.player?.let { p ->
                val dur = p.duration.coerceAtLeast(0L)
                if (!dragging) {
                    seek.max = dur.toInt()
                    seek.progress = p.currentPosition.toInt()
                }
                timeText.text = "${Ui.fmt(p.currentPosition)} / ${Ui.fmt(dur)}"
                playBtn.text = if (p.isPlaying) "PAUSE" else "PLAY"
            }
            val on = Bus.running
            detVal.text = if (on) "ACTIVE" else "INACTIVE"
            detVal.setTextColor(if (on) Ui.OK else Ui.DIM)
            stateVal.text = Bus.state
            autoVal.text = if (Bus.autoReset) "ON" else "OFF"
            resetVal.text = Bus.resets.toString()
            detBtn.text = if (on) "STOP DETECTION" else "START DETECTION"
            errText.text = Bus.error ?: ""
            ui.postDelayed(this, 300)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Bus.autoReset = prefs.getBoolean("auto", true)
        Bus.pauseInEditor = prefs.getBoolean("pause", true)
        Bus.sens = prefs.getInt("sens", 5)
        Bus.cooldownMs = prefs.getInt("cool", 1200)
        buildUi()

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val i = Intent(this, MusicService::class.java)
        ContextCompat.startForegroundService(this, i)
        bindService(i, conn, Context.BIND_AUTO_CREATE)
        ui.post(ticker)
    }

    override fun onDestroy() {
        ui.removeCallbacks(ticker)
        unbindService(conn)
        super.onDestroy()
    }

    private fun mpm() = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    fun toggleDetect() {
        pendingLaunch = false
        if (Bus.running) {
            stopService(Intent(this, DetectService::class.java))
        } else {
            capture.launch(mpm().createScreenCaptureIntent())
        }
    }

    fun launchFlow() {
        if (Bus.running) {
            launchGd()
        } else {
            pendingLaunch = true
            capture.launch(mpm().createScreenCaptureIntent())
        }
    }

    private fun launchGd() {
        val i = packageManager.getLaunchIntentForPackage(GD_PACKAGE)
        if (i == null) {
            toast("Geometry Dash is not installed")
            return
        }
        startActivity(i)
    }

    private fun load(uri: Uri) {
        svc?.load(uri)
        trackTitle.text = nameOf(uri)
    }

    private fun nameOf(u: Uri): String =
        contentResolver.query(u, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
        } ?: "Unknown file"

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
