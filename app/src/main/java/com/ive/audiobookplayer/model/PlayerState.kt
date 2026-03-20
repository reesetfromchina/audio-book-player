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
    val showChapterList: Boolean = false,
    val sleepTimerEndTime: Long = 0L,
    val sleepTimerLabel: String = "",
    val currentCoverBitmap: Bitmap? = null,

)
data class LibraryState(
    val audiobooks: List<Audiobook> = emptyList(),
    val bookmarks: Map<String, BookmarkEntry> = emptyMap(),
    val libraryGroups: LibraryGroups = LibraryGroups(),
    val isLoaded: Boolean = false,
    val isScanning: Boolean = false,
    val isBackgroundScanning: Boolean = false,
    val isInitializing: Boolean = true,
)