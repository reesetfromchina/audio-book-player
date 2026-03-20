package com.ive.audiobookplayer.model

import kotlinx.serialization.Serializable

@Serializable
data class Audiobook(
    val id: String,
    val title: String,
    val author: String = "",
    val chapters: List<Chapter>,
    val coverUri: String? = null,
)

@Serializable
data class Chapter(
    val uri: String,
    val title: String,
    val index: Int,
    val durationMs: Long = 0L
) {
    val isUnknownDuration get() = durationMs == 0L
}

@Serializable
data class BookmarkEntry(
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMs: Long,
    val lastPlayedAt: Long = System.currentTimeMillis()
)