package com.ive.audiobookplayer.model

import androidx.media3.session.MediaController

data class PlayerState(
    val controller : MediaController? = null,
    val isPlaying: Boolean = false,
    val currentTitle: String = "No Audiobook playing",
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1f,
)