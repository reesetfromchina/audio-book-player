package com.ive.audiobookplayer.utils

import com.ive.audiobookplayer.model.Chapter

object CueParser {
    fun parse(cueContent: String, audioUri: String): List<Chapter> {
        val tracks = mutableListOf<Pair<String, Long>>()
        var currentTitle = ""

        cueContent.lines().forEachIndexed { _, line ->
            val trimmed = line.trim()

            when {
                trimmed.startsWith("    TITLE") || (trimmed.startsWith("TITLE") ) -> {
                    currentTitle = trimmed
                        .substringAfter("\"")
                        .substringBefore("\"")
                }

                trimmed.startsWith("TRACK") -> {
                    val trackNumber = trimmed.split(" ")[1].toIntOrNull() ?: (tracks.size + 1)
                    currentTitle = "Chapter $trackNumber"
                }

                trimmed.startsWith("INDEX 01") -> {
                    val time = trimmed.substringAfter("INDEX 01").trim()
                    val ms = parseTimeToMs(time)
                    tracks.add(currentTitle to ms)
                }
            }
        }

        if (tracks.isEmpty()) {
            return emptyList()
        }

        return tracks.mapIndexed { index, (title, startMs) ->
            val nextStartMs = if (index + 1 < tracks.size) tracks[index + 1].second else null
            val durationMs = if (nextStartMs != null) nextStartMs - startMs else {
                0L
            }

            Chapter(
                uri = audioUri,
                title = title.ifEmpty { "Chapter ${index + 1}" },
                index = index,
                durationMs = durationMs,
                startMs = startMs
            )
        }
    }

    private fun parseTimeToMs(time: String): Long {
        return try {
            val parts = time.split(":")
            if (parts.size != 3) return 0L
            val minutes = parts[0].toLongOrNull() ?: 0L
            val seconds = parts[1].toLongOrNull() ?: 0L
            val frames = parts[2].toLongOrNull() ?: 0L
            (minutes * 60 * 1000) + (seconds * 1000) + (frames * 1000 / 75)
        } catch (e: Exception) {
            0L
        }
    }
}