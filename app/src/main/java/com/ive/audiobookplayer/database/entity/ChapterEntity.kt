package com.ive.audiobookplayer.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapters",
    foreignKeys = [ForeignKey(
        entity = AudiobookEntity::class,
        parentColumns = ["id"],
        childColumns = ["audiobookId"],
        onDelete = ForeignKey.CASCADE, // delete book -> delete all its chapters
    )],
    indices = [Index("audiobookId")]
)
data class ChapterEntity(
    @PrimaryKey
    val id: String,
    val audiobookId: String,
    val title: String,
    val uri: String,
    val index: Int,
    val durationMs: Long = 0L,
    val startMs: Long = 0L,
)