package com.ive.audiobookplayer.utils

import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry


fun calculateProgress(audiobook: Audiobook, bookmark: BookmarkEntry?): Float {
    bookmark ?: return 0f

    val totalDuration = audiobook.chapters.sumOf { it.durationMs }

    val effectiveTotal = if (totalDuration > 0L) totalDuration else bookmark.totalDurationMs
    if (effectiveTotal <= 0L) return 0f

    val completedDuration = audiobook.chapters
        .take(bookmark.chapterIndex)
        .sumOf { it.durationMs }

    return (((completedDuration + bookmark.positionMs).toFloat() / effectiveTotal) * 100f)
        .coerceIn(0f, 100f)
}