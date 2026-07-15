package com.ive.audiobookplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.utils.toFormattedDuration

@Composable
fun AudiobookProgressRow(
    bookmark: BookmarkEntry,
    progress: Float,
    isFinished: Boolean
) {

    LinearProgressIndicator(

        progress = { (progress / 100f).coerceIn(0f, 1f) },

        color = if (isFinished)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.primary,
        drawStopIndicator = {},
        gapSize = 0.dp,
        strokeCap = StrokeCap.Butt,
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            if (isFinished) "Finished"
            else "Ch.${bookmark.chapterIndex + 1} — ${bookmark.positionMs.toFormattedDuration()}",

            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "%.0f%%".format(progress),
            style = MaterialTheme.typography.bodySmall,
            color = if (isFinished)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.primary
        )
    }
}