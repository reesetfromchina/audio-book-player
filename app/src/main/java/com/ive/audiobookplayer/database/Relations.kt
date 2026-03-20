package com.ive.audiobookplayer.database

import androidx.room.Embedded
import androidx.room.Relation

data class AudiobookWithChapters(
    @Embedded
    val audiobook: AudiobookEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "audiobookId"
    )

    val chapters: List<ChapterEntity>
)