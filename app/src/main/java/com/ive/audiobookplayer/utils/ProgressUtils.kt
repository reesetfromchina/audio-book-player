package com.ive.audiobookplayer.utils

import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry


fun calculateProgress(audiobook: Audiobook, bookmark: BookmarkEntry?): Float {
    bookmark ?: return 0f

    val totalDuration = audiobook.chapters.sumOf { it.durationMs }

//    val effectiveTotal = if (totalDuration > 0L) totalDuration else bookmark.totalDurationMs
    if (totalDuration <= 0L) return 0f

    val completedDuration = audiobook.chapters
        .take(bookmark.chapterIndex)
        .sumOf { it.durationMs }

    return (((completedDuration + bookmark.positionMs).toFloat() / totalDuration) * 100f)
        .coerceIn(0f, 100f)
}

fun calculateTimeRemaining(audiobook: Audiobook, bookmark: BookmarkEntry?, speed: Float) : Long{
    if (speed <=0f) return 0L
    val totalDuration = audiobook.chapters.sumOf { it.durationMs }
    if (totalDuration < 0L) return 0L

    val completedDuration = audiobook.chapters
        .take(bookmark?.chapterIndex ?: 0)
        .sumOf { it.durationMs }

    val positionMs = bookmark?.positionMs ?: 0L
    val remainingMs = totalDuration - completedDuration - positionMs

    return (remainingMs / speed).toLong().coerceAtLeast(0L)
}

fun formatDuration(ms: Long) : String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when{
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

fun formatDurationSeconds(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun Long.toFormattedDuration(): String{
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}