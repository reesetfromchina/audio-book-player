package com.ive.audiobookplayer.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "audiobooks")
data class AudiobookEntity(
    @PrimaryKey
    val id:String,
    val title: String,
    val coverUri: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val isNew: Boolean = true
)

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
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMs: Long,
    val playbackSpeed: Float = 1f,
    val lastPlayedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = AudiobookEntity::class,
        parentColumns = ["id"],
        childColumns = ["audiobookId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("audiobookId")])
data class NoteEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val audiobookId: String,
    val chapterIndex: Int,
    val positionMs: Long,
    val noteText: String,
    val createdAt : Long = System.currentTimeMillis(),
)
