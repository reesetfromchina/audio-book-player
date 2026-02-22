package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import androidx.compose.material3.Icon
@Composable
fun PlaybackSpeedControl(
    controller: MediaController,
    currentSpeed: Float,
    sleepTimerLabel: String,
    onSleepTimerClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
        ) {
            Text("Playback Speed: ${"%.2f".format(currentSpeed)}x")
            Slider(
                value = currentSpeed,
                valueRange = 0.5f..2.5f,
                onValueChange = { controller.setPlaybackSpeed(it) }
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onSleepTimerClick) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = "Sleep timer",
                    tint = if (sleepTimerLabel.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (sleepTimerLabel.isNotEmpty()) {
                Text(
                    text = sleepTimerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}