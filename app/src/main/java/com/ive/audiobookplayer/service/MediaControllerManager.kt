package com.ive.audiobookplayer.service

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ive.audiobookplayer.model.Audiobook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.net.toUri

class MediaControllerManager(private val application: Application) {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    init {
        connectToService()
    }

    @OptIn(UnstableApi::class)
    private fun connectToService() {
        val sessionToken =
            SessionToken(application, ComponentName(application, PlaybackService::class.java))

        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                _controller.value = controllerFuture?.get()
            } catch (e: Exception) {
                Log.e("MediaController", "Failed to connect to PlaybackService", e)
            }
        }, MoreExecutors.directExecutor())
    }

    fun prepareAudiobook(
        audiobook: Audiobook,
        startChapterIndex: Int = 0,
        startPositionMs: Long = 0L,
        coverUri: Uri? = null
    ){
        val controller = _controller.value ?: return

        val mediaItems =  audiobook.chapters.map{ chapter ->
            MediaItem.Builder()
                .setMediaId(chapter.uri)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(chapter.uri.toUri())
                        .build()
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(chapter.title)
                        .setExtras(Bundle().apply { putString("audiobookId", audiobook.id) })
                        .setAlbumTitle(audiobook.title)
                        .setArtworkUri(coverUri)
                        .build()
                ).apply {
                    if(chapter.startMs>0L){
                        setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(chapter.startMs)
                                .apply {
                                    if(chapter.durationMs > 0L){
                                        setEndPositionMs(chapter.startMs + chapter.durationMs)
                                    }
                                }.build()
                        )
                    }
                }.build()
        }

        controller.setMediaItems(mediaItems, startChapterIndex, startPositionMs)
        controller.playWhenReady = false
        controller.prepare()
    }


    /**
     * Plays an audiobook, restoring position from a bookmark if provided.
     */
    fun playAudiobook(
        audiobook: Audiobook,
        startChapterIndex: Int = 0,
        startPositionMs: Long = 0L,
        coverUri: Uri? = null
    ) {
        val controller = _controller.value
        if (controller == null) {
            Log.d("MediaController", "Controller not ready yet")
            return
        }

        val mediaItems = audiobook.chapters.mapIndexed { index, chapter ->
            val clippingConfig = if (chapter.durationMs > 0L) {
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(chapter.startMs)
                    .setEndPositionMs(chapter.startMs + chapter.durationMs)
                    .build()
            } else if (index < audiobook.chapters.size - 1) {
                val nextChapter = audiobook.chapters[index + 1]
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(chapter.startMs)
                    .setEndPositionMs(nextChapter.startMs)
                    .build()
            } else {
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(chapter.startMs)
                    .build()
            }
            val displayTitle = if (chapter.title.startsWith("Chapter ")) {
                audiobook.title
            } else {
                chapter.title
            }


            MediaItem.Builder()
                .setUri(chapter.uri.toUri())
                .setMediaId(chapter.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(displayTitle)
                        .setExtras(Bundle().apply { putString("audiobookId", audiobook.id) })
                        .setSubtitle(audiobook.title)
                        .setAlbumTitle(audiobook.title)
                        .setArtworkUri(coverUri)
                        .build()
                )
                .apply {
                    if (clippingConfig != null) {
                        setClippingConfiguration(clippingConfig)
                    }
                }
                .build()
        }


        controller.setMediaItems(
            mediaItems, startChapterIndex, startPositionMs
        )
//        controller.playWhenReady = false // never autoplay when switching books
        controller.prepare()
        controller.play()
    }


    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}