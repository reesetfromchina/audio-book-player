package com.ive.audiobookplayer.model

import android.graphics.Bitmap
import androidx.media3.session.MediaController

data class PlayerState(
    val controller: MediaController? = null,
    val isPlaying: Boolean = false,
    val currentTitle: String = "No Audiobook playing",
    val currentBookTitle: String = "",
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val playbackSpeed: Float = 1f,
    val currentAudiobook: Audiobook? = null,
    val currentChapterIndex: Int = 0,
    val isScanning: Boolean = false,
    val isBackgroundScanning: Boolean = false,

    val audiobooks: List<Audiobook> = emptyList(),
    val bookmarks: Map<String, BookmarkEntry> = emptyMap(),
    val isLibraryLoaded: Boolean = false,
    val currentCoverBitmap: Bitmap? = null,
    val showChapterList: Boolean = false
)