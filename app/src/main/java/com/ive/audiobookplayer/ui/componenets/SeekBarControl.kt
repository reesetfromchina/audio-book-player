package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import com.ive.audiobookplayer.utils.TimeFormatter

@Composable
fun SeekBarControl(
    controller: MediaController,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float = 1f,
    onPositionChange: (Long) -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val displayPosition = if (isSeeking) seekPosition else currentPosition.toFloat()
    val safeDuration = duration.coerceAtLeast(1L).toFloat()
    val adjustedDuration = (safeDuration / playbackSpeed).toLong()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Slider(
            value = (displayPosition / safeDuration).coerceIn(0f, 1f),
            valueRange = 0f..1f,
            onValueChange = { fraction ->
                isSeeking = true
                seekPosition = fraction * safeDuration
            },
            onValueChangeFinished = {
                controller.seekTo(seekPosition.toLong())
                onPositionChange(seekPosition.toLong())
                isSeeking = false
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = TimeFormatter.formatTime(displayPosition.toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (playbackSpeed != 1f) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = TimeFormatter.formatTime(adjustedDuration),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = TimeFormatter.formatTime(safeDuration.toLong()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
            } else {
                Text(
                    text = TimeFormatter.formatTime(safeDuration.toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
