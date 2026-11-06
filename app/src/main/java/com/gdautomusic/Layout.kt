package com.gdautomusic

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import androidx.appcompat.widget.SwitchCompat

fun MainActivity.buildUi() {
    val pad = Ui.dp(this, 16)
    val root = LinearLayout(this)
    root.orientation = LinearLayout.VERTICAL
    root.setPadding(pad, pad, pad, pad)

    val title = Ui.label(this, "GD AUTO MUSIC RESET", 18f, Ui.ORANGE)
    title.letterSpacing = 0.12f
    root.addView(title)
    root.addView(Ui.label(this, "Custom MP3 for the Geometry Dash editor", 12f, Ui.DIM))

    val cols = LinearLayout(this)
    cols.orientation = LinearLayout.HORIZONTAL
    cols.setPadding(0, pad, 0, pad)
    cols.addView(playerCard(), colLp())
    cols.addView(detectCard(), colLp())
    root.addView(cols)

    root.addView(Ui.row(this, Ui.btn(this, "LAUNCH GEOMETRY DASH", Ui.ORANGE) { launchFlow() }))

    val sv = ScrollView(this)
    sv.setBackgroundColor(Ui.BG)
    sv.addView(root)
    setContentView(sv)
}

private fun MainActivity.colLp(): LinearLayout.LayoutParams {
    val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
    val m = Ui.dp(this, 6)
    lp.setMargins(m, m, m, m)
    return lp
}

fun MainActivity.playerCard(): LinearLayout {
    val c = Ui.card(this)
    c.addView(Ui.header(this, "Player"))

    trackTitle = Ui.label(this, "No music selected", 20f, Color.WHITE)
    trackTitle.maxLines = 2
    trackTitle.setPadding(0, 0, 0, Ui.dp(this, 12))
    c.addView(trackTitle)

    seek = SeekBar(this)
    seek.progressTintList = ColorStateList.valueOf(Ui.ORANGE)
    seek.thumbTintList = ColorStateList.valueOf(Ui.ORANGE)
    seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
            if (fromUser) svc?.player?.seekTo(p.toLong())
        }
        override fun onStartTrackingTouch(s: SeekBar) { dragging = true }
        override fun onStopTrackingTouch(s: SeekBar) { dragging = false }
    })
    c.addView(seek)

    timeText = Ui.label(this, "0:00 / 0:00", 12f, Ui.DIM)
    timeText.gravity = Gravity.END
    c.addView(timeText)

    playBtn = Ui.btn(this, "PLAY", Ui.RED) { svc?.togglePlay() }
    c.addView(Ui.row(
        this, playBtn,
        Ui.btn(this, "RESET", Ui.RED) { svc?.resetToZero() },
        Ui.btn(this, "STOP", Ui.LINE) {
            svc?.player?.pause()
            svc?.player?.seekTo(0)
        }
    ))
    c.addView(Ui.row(this, Ui.btn(this, "PICK MP3", Ui.LINE) { picker.launch(arrayOf("audio/*")) }))

    c.addView(Ui.bar(this, "Volume", 0, 100, prefs.getInt("vol", 100)) { p ->
        svc?.player?.volume = p / 100f
        prefs.edit().putInt("vol", p).apply()
    })

    val loop = SwitchCompat(this)
    loop.text = "Loop"
    loop.setTextColor(Ui.TEXT)
    loop.isChecked = prefs.getBoolean("loop", true)
    loop.setOnCheckedChangeListener { _, on ->
        svc?.setLoop(on)
        prefs.edit().putBoolean("loop", on).apply()
    }
    c.addView(loop)
    return c
}

fun MainActivity.detectCard(): LinearLayout {
    val c = Ui.card(this)
    c.addView(Ui.header(this, "Detection"))

    val a = Ui.statusRow(this, "Screen Detection")
    detVal = a.second
    c.addView(a.first)
    val b = Ui.statusRow(this, "Editor State")
    stateVal = b.second
    c.addView(b.first)
    val d = Ui.statusRow(this, "Auto Reset")
    autoVal = d.second
    c.addView(d.first)
    val e = Ui.statusRow(this, "Resets")
    resetVal = e.second
    c.addView(e.first)

    val s1 = SwitchCompat(this)
    s1.text = "Auto reset"
    s1.setTextColor(Ui.TEXT)
    s1.isChecked = Bus.autoReset
    s1.setOnCheckedChangeListener { _, on ->
        Bus.autoReset = on
        prefs.edit().putBoolean("auto", on).apply()
    }
    c.addView(s1)

    val s2 = SwitchCompat(this)
    s2.text = "Pause music when playtest ends"
    s2.setTextColor(Ui.TEXT)
    s2.isChecked = Bus.pauseInEditor
    s2.setOnCheckedChangeListener { _, on ->
        Bus.pauseInEditor = on
        prefs.edit().putBoolean("pause", on).apply()
    }
    c.addView(s2)

    c.addView(Ui.bar(this, "Sensitivity (higher = looser)", 2, 15, Bus.sens) { p ->
        Bus.sens = p
        prefs.edit().putInt("sens", p).apply()
    })
    c.addView(Ui.bar(this, "Cooldown (ms)", 500, 3000, Bus.cooldownMs) { p ->
        Bus.cooldownMs = p
        prefs.edit().putInt("cool", p).apply()
    })

    errText = Ui.label(this, "", 12f, Color.parseColor("#E06C5A"))
    errText.setPadding(0, Ui.dp(this, 8), 0, 0)
    c.addView(errText)

    detBtn = Ui.btn(this, "START DETECTION", Ui.RED) { toggleDetect() }
    c.addView(Ui.row(this, detBtn))
    return c
}
