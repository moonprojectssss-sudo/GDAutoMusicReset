package com.gdautomusic

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class Detector {
    private var buf: ByteBuffer? = null
    private var w = 0
    private var h = 0
    private var stride = 0

    private var mode = 0 // 0 = editor, 1 = playtest
    private var pSince = 0L
    private var aSince = 0L
    private var baseAt = 0L
    private var baseline: IntArray? = null
    private var diverged = false
    private var divSince = 0L
    private var matches = 0
    private var lastReset = 0L

    private fun rgb(x: Int, y: Int): Int {
        val b = buf ?: return 0
        val xx = x.coerceIn(0, w - 1)
        val yy = y.coerceIn(0, h - 1)
        val i = yy * stride + xx * 4
        val r = b.get(i).toInt() and 0xFF
        val g = b.get(i + 1).toInt() and 0xFF
        val bl = b.get(i + 2).toInt() and 0xFF
        return (r shl 16) or (g shl 8) or bl
    }

    private fun green(x: Int, y: Int): Boolean {
        val c = rgb(x, y)
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return g > 120 && g > r + 30 && g > b + 50
    }

    private fun luma(x: Int, y: Int): Int {
        val c = rgb(x, y)
        return (((c shr 16) and 0xFF) * 3 + ((c shr 8) and 0xFF) * 6 + (c and 0xFF)) / 10
    }

    // Green round button (the playtest STOP button) at a screen fraction
    private fun buttonAt(fx: Double, fy: Double): Boolean {
        val cx = (fx * w).toInt()
        val cy = (fy * h).toInt()
        val r = (0.0175 * w).toInt().coerceAtLeast(3)
        var hit = 0
        for (a in 0 until 8) {
            val ang = a * Math.PI / 4
            if (green(cx + (r * cos(ang)).toInt(), cy + (r * sin(ang)).toInt())) hit++
        }
        val far = (0.045 * w).toInt()
        val bgGreen = green(cx - far, cy) && green(cx + far, cy)
        return hit >= 5 && !bgGreen
    }

    private fun thumb(): IntArray {
        val gx = 24
        val gy = 14
        val out = IntArray(gx * gy)
        val x0 = (0.20 * w).toInt()
        val x1 = (0.92 * w).toInt()
        val y0 = (0.12 * h).toInt()
        val y1 = (0.88 * h).toInt()
        for (j in 0 until gy) for (i in 0 until gx) {
            out[j * gx + i] = luma(x0 + (x1 - x0) * i / gx, y0 + (y1 - y0) * j / gy)
        }
        return out
    }

    private fun diff(a: IntArray, b: IntArray): Double {
        var s = 0
        for (i in a.indices) s += abs(a[i] - b[i])
        return s.toDouble() / a.size
    }

    fun feed(buffer: ByteBuffer, width: Int, height: Int, rowStride: Int, now: Long) {
        buf = buffer
        w = width
        h = height
        stride = rowStride

        val present = buttonAt(0.1442, 0.3444)
        if (present) {
            if (pSince == 0L) pSince = now
            aSince = 0L
        } else {
            if (aSince == 0L) aSince = now
            pSince = 0L
        }

        if (mode == 0 && present && now - pSince >= 200) enter(now)
        else if (mode == 1 && !present && now - aSince >= 400) leave()
        if (mode == 1) track(now)
    }

    private fun enter(now: Long) {
        mode = 1
        Bus.state = "PLAYTEST"
        baseline = null
        baseAt = now + 300
        diverged = false
        divSince = 0L
        matches = 0
        lastReset = now
        Bus.resets++
        if (Bus.autoReset) Bus.act { resetToZero() }
    }

    private fun leave() {
        mode = 0
        Bus.state = "EDITOR"
        if (Bus.autoReset && Bus.pauseInEditor) Bus.act {
            player.pause()
            player.seekTo(0)
        }
    }

    // Respawn = screen diverged from the attempt-start frame, then matches it again
    private fun track(now: Long) {
        val base = baseline
        if (base == null) {
            if (now >= baseAt) baseline = thumb()
            return
        }
        val d = diff(thumb(), base)
        val matchThr = Bus.sens * 0.4
        val divThr = Bus.sens * 1.2
        if (!diverged) {
            if (d > divThr) {
                if (divSince == 0L) divSince = now
                if (now - divSince >= 500) diverged = true
            } else {
                divSince = 0L
            }
        } else {
            if (d <= matchThr) matches++ else matches = 0
            if (matches >= 2 && now - lastReset >= Bus.cooldownMs) {
                diverged = false
                matches = 0
                divSince = 0L
                lastReset = now
                Bus.resets++
                if (Bus.autoReset) Bus.act { resetToZero() }
            }
        }
    }
}
