package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.utils.calculateTimeRemaining
import com.ive.audiobookplayer.utils.formatDuration
@Composable
fun PlayerBottomControls(
    playbackSpeed: Float,
    sleepTimerLabel: String,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    currentChapterIndex: Int,
    currentPositionMs: Long,
    currentChapterTitle: String,
    onSaveNote: (Int, Long, String) -> Unit,
    onPause: () -> Unit,
) {
    var showNoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSleepTimerClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Bedtime,
                    contentDescription = "Sleep timer",
                    modifier = Modifier.size(20.dp),
                    tint = if (sleepTimerLabel.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (sleepTimerLabel.isNotEmpty()) sleepTimerLabel else "Sleep",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sleepTimerLabel.isNotEmpty())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSpeedClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Outlined.Speed,
                    contentDescription = "Playback speed",
                    modifier = Modifier.size(20.dp),
                    tint = if (playbackSpeed != 1f)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (playbackSpeed == 1f) "Speed" else "${playbackSpeed}×",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (playbackSpeed != 1f)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { showNoteDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.EditNote,
                    contentDescription = "Add note",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Note",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showNoteDialog) {
        LaunchedEffect(Unit) {
            onPause()
        }
        NoteBottomSheet(
            positionMs = currentPositionMs,
            onDismiss = { showNoteDialog = false },
            onSave = { noteText ->
                onSaveNote(currentChapterIndex, currentPositionMs, noteText)
                showNoteDialog = false
            },
            chapterTitle = currentChapterTitle,
        )
    }
}