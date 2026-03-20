package com.ive.audiobookplayer.database

import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.model.Chapter

fun AudiobookWithChapters.toAudiobook(): Audiobook {
    return Audiobook(
        id = audiobook.id,
        title = audiobook.title,
        coverUri = audiobook.coverUri,
        chapters = chapters
            .sortedBy { it.index }
            .map { chapter ->
                Chapter(
                    uri = chapter.uri,
                    title = chapter.title,
                    index = chapter.index,
                    durationMs = chapter.durationMs
                )
            }
    )
}

fun BookmarkEntity.toBookmarkEntry(): BookmarkEntry {
    return BookmarkEntry(
        audiobookId = audiobookId,
        chapterIndex = chapterIndex,
        positionMs = positionMs,
        lastPlayedAt = lastPlayedAt
    )
}