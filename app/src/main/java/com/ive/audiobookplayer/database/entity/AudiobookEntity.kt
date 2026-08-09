package com.ive.audiobookplayer.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audiobooks")
data class AudiobookEntity(
    @PrimaryKey
    val id:String,
    val title: String,
    val author: String = "",
    val coverUri: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val isNew: Boolean = true
)
