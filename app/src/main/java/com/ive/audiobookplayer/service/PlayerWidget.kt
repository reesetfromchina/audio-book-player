package com.ive.audiobookplayer.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.ive.audiobookplayer.MainActivity
import com.ive.audiobookplayer.R

class PlayerWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val title = prefs.getString("title", "No book playing") ?: "No book playing"
            val chapter = prefs.getString("chapter", "") ?: ""
            val progress = prefs.getInt("progress", 0)
            val isPlaying = prefs.getBoolean("isPlaying", false)
            val coverPath = prefs.getString("coverPath", null)

            val views = RemoteViews(context.packageName, R.layout.widget_player)

            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_chapter, chapter)
            views.setProgressBar(R.id.widget_progress, 100, progress, false)
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (isPlaying) R.drawable.ic_widget_pause
                else R.drawable.ic_widget_play
            )

            if (coverPath != null) {
                try {
                    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bitmap = BitmapFactory.decodeFile(coverPath, options)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_cover, bitmap)
                    } else {
                        views.setImageViewResource(R.id.widget_cover, R.drawable.ic_notification)
                    }
                } catch (e: Exception) {
                    views.setImageViewResource(R.id.widget_cover, R.drawable.ic_notification)
                }
            }

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context, 1, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            val playPauseIntent = Intent(context, PlayerWidget::class.java).apply {
                action = "ACTION_PLAY_PAUSE"
            }
            val playPausePending = PendingIntent.getBroadcast(
                context, 0, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_play_pause, playPausePending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }




    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_PLAY_PAUSE") {
            val serviceIntent = Intent(context, PlaybackService::class.java).apply {
                action = "ACTION_PLAY_PAUSE"
            }
            context.startService(serviceIntent)
        }
    }
}