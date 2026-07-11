package com.ive.audiobookplayer.database

import androidx.room.Embedded
import androidx.room.Relation


// When I ask for an audiobook, I also want all of its chapters.
data class AudiobookWithChapters(
    @Embedded
    val audiobook: AudiobookEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "audiobookId"
    )

    val chapters: List<ChapterEntity>
)