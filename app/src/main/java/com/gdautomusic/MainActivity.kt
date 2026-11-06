package com.gdautomusic

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gdautomusic.Ui.BG
import com.gdautomusic.Ui.CARD
import com.gdautomusic.Ui.ORANGE
import com.gdautomusic.Ui.RED

class MainActivity : AppCompatActivity() {

    companion object {
        const val GD_PACKAGE = "com.robtopx.geometryjump"
    }

    private var svc: MusicService? = null
    private val ui = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("gd", Context.MODE_PRIVATE) }

    private lateinit var trackTitle: TextView
    private lateinit var timeText: TextView
    private lateinit var seek: SeekBar
    private lateinit var playBtn: Button
    private var dragging = false

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            val s = (b as MusicService.LocalBinder).service
            svc = s
            s.onError = { msg -> runOnUiThread { toast(msg) } }
            s.player.volume = prefs.getInt("vol", 100) / 100f
            s.setLoop(prefs.getBoolean("loop", true))
            if (s.player.mediaItemCount == 0) {
                prefs.getString("uri", null)?.let { load(Uri.parse(it)) }
            }
        }

        override fun onServiceDisconnected(n: ComponentName) {
            svc = null
        }
    }

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        prefs.edit().putString("uri", uri.toString()).apply()
        load(uri)
    }

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
            ui.postDelayed(this, 300)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(40, 80, 40, 40)
        }

        root.addView(TextView(this).apply {
            text = "GD AUTO MUSIC RESET"
            setTextColor(ORANGE)
            textSize = 16f
            letterSpacing = 0.15f
        })

        trackTitle = TextView(this).apply {
            text = "No music selected"
            setTextColor(Color.WHITE)
            textSize = 24f
            setPadding(0, 60, 0, 40)
            maxLines = 3
        }
        root.addView(trackTitle)

        seek = SeekBar(this)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                if (fromUser) svc?.player?.seekTo(p.toLong())
            }

            override fun onStartTrackingTouch(s: SeekBar) {
                dragging = true
            }

            override fun onStopTrackingTouch(s: SeekBar) {
                dragging = false
            }
        })
        root.addView(seek)

        timeText = TextView(this).apply {
            text = "0:00 / 0:00"
            setTextColor(Color.LTGRAY)
            gravity = Gravity.END
        }
        root.addView(timeText)

        playBtn = btn("PLAY", RED) { svc?.togglePlay() }
        root.addView(row(
            playBtn,
            btn("RESET", RED) { svc?.resetToZero() },
            btn("STOP", CARD) {
                svc?.player?.pause()
                svc?.player?.seekTo(0)
            }
        ))

        root.addView(row(btn("PICK MP3", CARD) { picker.launch(arrayOf("audio/*")) }))

        root.addView(TextView(this).apply {
            text = "Volume"
            setTextColor(Color.LTGRAY)
            setPadding(0, 40, 0, 0)
        })
        root.addView(SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("vol", 100)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                    svc?.player?.volume = p / 100f
                    prefs.edit().putInt("vol", p).apply()
                }

                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) {}
            })
        })

        root.addView(CheckBox(this).apply {
            text = "Loop"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("loop", true)
            setOnCheckedChangeListener { _, on ->
                svc?.setLoop(on)
                prefs.edit().putBoolean("loop", on).apply()
            }
        })

        root.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        root.addView(row(btn("LAUNCH GEOMETRY DASH", ORANGE) { launchGd() }))

        setContentView(root)
    }

    private fun launchGd() {
        val i = packageManager.getLaunchIntentForPackage(GD_PACKAGE)
        if (i == null) {
            toast("Geometry Dash is not installed")
            return
        }
        svc?.resetToZero()
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

    private fun btn(t: String, color: Int, f: () -> Unit) = Ui.btn(this, t, color, f)

    private fun row(vararg v: View) = Ui.row(this, *v)

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()
}
