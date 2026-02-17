package com.ive.audiobookplayer.service

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MediaControllerManager(private val application: Application) {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken =
            SessionToken(application, ComponentName(application, PlaybackService::class.java))

        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()

        controllerFuture?.addListener({
            _controller.value = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }

    fun playPlaylist(uris: List<Uri>) {
        val controller = _controller.value ?: return

        val mediaItems = uris.map { uri ->
            val fileName = uri.lastPathSegment ?: "Unknown track"

            MediaItem.Builder()
                .setUri(uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(fileName).build()).build()
        }
        controller.setMediaItems(mediaItems)
        controller.prepare()
        controller.play()
    }

    fun release(){
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}