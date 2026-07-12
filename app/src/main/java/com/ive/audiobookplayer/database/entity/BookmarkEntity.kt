package com.ive.audiobookplayer.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMs: Long,
    val playbackSpeed: Float = 1f,
    val lastPlayedAt: Long = System.currentTimeMillis(),
)
