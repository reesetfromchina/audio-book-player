package com.ive.audiobookplayer.model

import kotlinx.serialization.Serializable

@Serializable
data class AppBackup(val version: Int = 1,
    val exportDate: Long = System.currentTimeMillis(),
    val bookmarks: List<BookmarkBackup>)


@Serializable
data class BookmarkBackup(
    val bookTitle: String,
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMS: Long,
    val playbackSpeed: Float,
    val lastPlayedAt: Long,
)