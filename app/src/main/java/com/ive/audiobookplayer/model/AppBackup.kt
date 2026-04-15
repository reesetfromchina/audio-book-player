package com.ive.audiobookplayer.model

import androidx.compose.material3.FabPosition
import kotlinx.serialization.Serializable

@Serializable
data class AppBackup(val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val bookmarks: List<BookmarkBackup>,
    val notes: List<NoteBackup>)



@Serializable
data class BookmarkBackup(
    val bookTitle: String,
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMS: Long,
    val playbackSpeed: Float,
    val lastPlayedAt: Long,
    val customCoverBase64: String? = null
)


@Serializable
data class NoteBackup(
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMS: Long,
    val noteText: String,
)