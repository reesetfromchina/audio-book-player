package com.ive.audiobookplayer.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "notes",
    foreignKeys = [ForeignKey(
        entity = AudiobookEntity::class,
        parentColumns = ["id"],
        childColumns = ["audiobookId"],
        onDelete = ForeignKey.Companion.CASCADE
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