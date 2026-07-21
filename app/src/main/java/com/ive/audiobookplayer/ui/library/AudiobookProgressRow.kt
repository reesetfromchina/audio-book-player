package com.ive.audiobookplayer.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.utils.toFormattedDuration
import java.util.Locale

@Composable
fun AudiobookProgressRow(
    bookmark: BookmarkEntry,
    progress: Float,
    isFinished: Boolean
) {
    LinearProgressIndicator(
        progress = { (progress / 100f).coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = if (isFinished)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        drawStopIndicator = {},
        gapSize = 0.dp,
        strokeCap = StrokeCap.Butt,
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            if (isFinished) "Finished"
            else "Ch.${bookmark.chapterIndex + 1} — ${bookmark.positionMs.toFormattedDuration()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isFinished) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Finished",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(
                "%.0f%%".format(Locale.US, progress),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}