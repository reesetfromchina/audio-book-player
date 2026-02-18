package com.ive.audiobookplayer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.ive.audiobookplayer.MainActivity
import com.ive.audiobookplayer.R


@UnstableApi
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "playback_channel",
                "Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        player = ExoPlayer.Builder(this).build()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(pendingIntent)
            .build()

        setupNotification()
    }

    private fun setupNotification() {
        val notificationListener = object : PlayerNotificationManager.NotificationListener {
            override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                stopForeground(STOP_FOREGROUND_LEGACY)
                stopSelf()
            }
        }

        notificationManager = PlayerNotificationManager.Builder(
            this,
            1,
            "playback_channel"
        )
            .setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
                override fun createCurrentContentIntent(player: Player): PendingIntent? {
                    Log.d("Notification", "createCurrentContentIntent called")
                    val intent = Intent(this@PlaybackService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    return PendingIntent.getActivity(
                        this@PlaybackService,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }

                override fun getCurrentContentTitle(player: Player): CharSequence {
                    return player.currentMediaItem?.mediaMetadata?.title ?: "No Track"
                }

                override fun getCurrentContentText(player: Player): CharSequence {
                    return player.currentMediaItem?.mediaMetadata?.artist ?: "Audiobook"
                }

                override fun getCurrentLargeIcon(
                    player: Player,
                    callback: PlayerNotificationManager.BitmapCallback
                ): Bitmap? {
                    return null
                }
            })
            .setChannelNameResourceId(R.string.app_name)
            .setChannelDescriptionResourceId(R.string.app_name)
            .setNotificationListener(notificationListener)
            .build()
            .also { manager ->
                manager.setPlayer(player)
                manager.setUseFastForwardAction(true)
                manager.setUseRewindAction(true)
                manager.setUseNextAction(true)
                manager.setUsePreviousAction(true)
                manager.setUsePlayPauseActions(true)
            }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        player?.playWhenReady = false
        player?.stop()
        stopSelf()
    }

    override fun onDestroy() {
        notificationManager?.setPlayer(null)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}