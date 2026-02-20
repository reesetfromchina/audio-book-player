package com.ive.audiobookplayer.model

import kotlinx.serialization.Serializable

@Serializable
data class Audiobook(
    val id: String,
    val title: String,
    val chapters: List<Chapter>,
    val coverUri: String? = null,
    val embeddedCoverUri: String? = null,
)

@Serializable
data class Chapter(val uri: String, val title: String, val index: Int, val durationMs: Long = 0L)

@Serializable
data class BookmarkEntry(
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMs: Long,
    val totalDurationMs: Long = 0L,
    val lastPlayedAt: Long = System.currentTimeMillis()
)