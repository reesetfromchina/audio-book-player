package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.session.MediaController
import com.ive.audiobookplayer.utils.TimeFormatter

@Composable
fun SeekBarControl(
    controller: MediaController,
    currentPosition: Long,
    duration: Long,
    onPositionChange: (Long) -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    val displayPosition = if (isSeeking) seekPosition else currentPosition.toFloat()
    val safeDuration = duration.coerceAtLeast(1L).toFloat()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(TimeFormatter.formatTime(displayPosition.toLong()))
            Text(TimeFormatter.formatTime(duration))
        }
    }
}