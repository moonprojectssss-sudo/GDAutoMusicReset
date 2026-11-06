package com.gdautomusic

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

object Ui {
    val BG = Color.parseColor("#1B1B1B")
    val CARD = Color.parseColor("#262626")
    val LINE = Color.parseColor("#3A3A3A")
    val RED = Color.parseColor("#A93226")
    val ORANGE = Color.parseColor("#C0662B")
    val TEXT = Color.parseColor("#E6E6E6")
    val DIM = Color.parseColor("#9A9A9A")
    val OK = Color.parseColor("#5FA75F")

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun shape(c: Context, color: Int, radius: Int, stroke: Int = 0): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(c, radius).toFloat()
        if (stroke != 0) d.setStroke(dp(c, 1), stroke)
        return d
    }

    fun card(c: Context): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.background = shape(c, CARD, 12, LINE)
        val p = dp(c, 16)
        l.setPadding(p, p, p, p)
        return l
    }

    fun label(c: Context, t: String, size: Float = 14f, color: Int = TEXT): TextView {
        val v = TextView(c)
        v.text = t
        v.textSize = size
        v.setTextColor(color)
        return v
    }

    fun header(c: Context, t: String): TextView {
        val v = label(c, t.uppercase(), 12f, ORANGE)
        v.letterSpacing = 0.12f
        v.setPadding(0, 0, 0, dp(c, 10))
        return v
    }

    fun btn(c: Context, t: String, color: Int, f: () -> Unit): Button {
        val b = Button(c)
        b.text = t
        b.setTextColor(Color.WHITE)
        b.textSize = 13f
        b.background = shape(c, color, 8)
        b.stateListAnimator = null
        b.setOnClickListener { f() }
        val lp = LinearLayout.LayoutParams(0, dp(c, 46), 1f)
        val m = dp(c, 4)
        lp.setMargins(m, m, m, m)
        b.layoutParams = lp
        return b
    }

    fun row(c: Context, vararg v: View): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.HORIZONTAL
        v.forEach { l.addView(it) }
        return l
    }

    fun statusRow(c: Context, name: String): Pair<LinearLayout, TextView> {
        val n = label(c, name, 13f, DIM)
        n.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        val v = label(c, "-", 13f, DIM)
        v.gravity = Gravity.END
        val r = row(c, n, v)
        r.setPadding(0, dp(c, 4), 0, dp(c, 4))
        return Pair(r, v)
    }

    fun bar(c: Context, name: String, min: Int, max: Int, init: Int, onChange: (Int) -> Unit): LinearLayout {
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(0, dp(c, 8), 0, 0)
        val t = label(c, "$name: $init", 12f, DIM)
        val s = SeekBar(c)
        s.min = min
        s.max = max
        s.progress = init
        s.progressTintList = ColorStateList.valueOf(ORANGE)
        s.thumbTintList = ColorStateList.valueOf(ORANGE)
        s.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) {
                t.text = "$name: $p"
                onChange(p)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        col.addView(t)
        col.addView(s)
        return col
    }

    fun fmt(ms: Long): String {
        val s = (ms.coerceAtLeast(0L) / 1000).toInt()
        return "%d:%02d".format(s / 60, s % 60)
    }
}
