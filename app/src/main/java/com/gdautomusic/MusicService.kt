package com.gdautomusic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MusicService : Service() {

    inner class LocalBinder : Binder() {
        val service: MusicService get() = this@MusicService
    }

    private val binder = LocalBinder()
    lateinit var player: ExoPlayer
        private set

    var onError: ((String) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_GAME)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false
            )
            repeatMode = Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    onError?.invoke("Playback failed: ${error.message}")
                }
            })
        }
        startAsForeground()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("music", "Music playback", NotificationManager.IMPORTANCE_LOW)
        )
        val n: Notification = NotificationCompat.Builder(this, "music")
            .setContentTitle("GD Auto Music Reset")
            .setContentText("Music service running")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(1, n)
        }
    }

    fun load(uri: Uri) {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    fun togglePlay() {
        if (player.mediaItemCount == 0) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun resetToZero() {
        if (player.mediaItemCount == 0) return
        player.seekTo(0)
        player.play()
    }

    fun setLoop(on: Boolean) {
        player.repeatMode = if (on) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}
