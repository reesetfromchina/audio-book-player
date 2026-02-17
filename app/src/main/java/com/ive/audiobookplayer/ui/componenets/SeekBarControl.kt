package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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


    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Slider(
            value = currentPosition.toFloat(),
            valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
            onValueChange = {
                controller.seekTo(it.toLong())
                onPositionChange(it.toLong())
            },

            )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(TimeFormatter.formatTime(currentPosition))
            Text(TimeFormatter.formatTime(duration))
        }
    }
}