package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.utils.calculateTimeRemaining
import com.ive.audiobookplayer.utils.formatDuration

@Composable
fun PlayerBottomControls(
    playbackSpeed: Float,
    sleepTimerLabel: String,
    audiobook: Audiobook?,
    bookmark: BookmarkEntry?,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSpeedClick) {
                Text(
                    text = if (playbackSpeed == 1f) "Speed: 1x" else "Speed: ${playbackSpeed}x",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onSleepTimerClick) {
                Text(
                    text = if (sleepTimerLabel.isNotEmpty()) "⏱ $sleepTimerLabel"
                    else "Sleep",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (sleepTimerLabel.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // remaining time
        if (audiobook != null) {
            val remainingMs = calculateTimeRemaining(audiobook, bookmark, playbackSpeed)
            if (remainingMs > 0) {
                val text = if (playbackSpeed != 1f) {
                    val naturalMs = calculateTimeRemaining(audiobook, bookmark, 1f)
                    "${formatDuration(remainingMs)} remaining |  ${formatDuration(naturalMs)} at 1x"
                } else {
                    "${formatDuration(remainingMs)} remaining"
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}