package com.gdautomusic

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.LinearLayout

object Ui {
    val BG = Color.parseColor("#1E1E1E")
    val CARD = Color.parseColor("#2B2B2B")
    val RED = Color.parseColor("#A93226")
    val ORANGE = Color.parseColor("#C0662B")

    fun btn(c: Context, t: String, color: Int, f: () -> Unit) = Button(c).apply {
        text = t
        setTextColor(Color.WHITE)
        setBackgroundColor(color)
        setOnClickListener { f() }
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            .apply { setMargins(6, 10, 6, 10) }
    }

    fun row(c: Context, vararg v: View) = LinearLayout(c).apply {
        orientation = LinearLayout.HORIZONTAL
        v.forEach { addView(it) }
    }

    fun fmt(ms: Long): String {
        val s = (ms.coerceAtLeast(0L) / 1000).toInt()
        return "%d:%02d".format(s / 60, s % 60)
    }
}
