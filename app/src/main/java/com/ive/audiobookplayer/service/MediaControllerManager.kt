package com.ive.audiobookplayer.service

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
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
            _controller.value = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }


    /**
     * Plays an audiobook, restoring position from a bookmark if provided.
     */
    fun playAudiobook(audiobook: Audiobook, bookmark: BookmarkEntry? = null, coverUri: Uri? = null) {
        val controller = _controller.value ?: return

        val wasPlaying = controller.isPlaying

        val mediaItems = audiobook.chapters.map { chapter ->
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
                        .setAlbumTitle(audiobook.title)
                        .setArtworkUri(coverUri)
                        .build()
                )
                .build()
        }

        controller.setMediaItems(
            mediaItems, bookmark?.chapterIndex ?: 0,
            bookmark?.positionMs ?: 0L
        )
        controller.playWhenReady = wasPlaying
        controller.prepare()
//        controller.play() dont autoplay
    }


    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}