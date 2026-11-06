package com.gdautomusic

import android.os.Handler
import android.os.Looper

object Bus {
    @Volatile var music: MusicService? = null
    @Volatile var running = false
    @Volatile var state = "OFF"
    @Volatile var resets = 0
    @Volatile var autoReset = true
    @Volatile var pauseInEditor = true
    @Volatile var sens = 5
    @Volatile var cooldownMs = 1200
    @Volatile var error: String? = null

    private val main = Handler(Looper.getMainLooper())

    // ExoPlayer must be used on the main thread
    fun act(f: MusicService.() -> Unit) {
        main.post {
            val m = music
            if (m != null) m.f()
        }
    }
}
