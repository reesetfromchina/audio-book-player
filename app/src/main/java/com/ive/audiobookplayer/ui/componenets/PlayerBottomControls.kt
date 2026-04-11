package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    audiobook: Audiobook?,
    bookmark: BookmarkEntry?,
    onSpeedClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    currentChapterIndex: Int,
    currentPositionMs: Long,
    currentChapterTitle: String,
    onSaveNote: (Int, Long, String) -> Unit,
) {
    var showNoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (audiobook != null) {
            val remainingMs = calculateTimeRemaining(audiobook, bookmark, playbackSpeed)
            if (remainingMs > 0) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "${formatDuration(remainingMs)} remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (playbackSpeed != 1f) {
                        val naturalMs = calculateTimeRemaining(audiobook, bookmark, 1f)
                        Text(
                            text = "  ·  ${formatDuration(naturalMs)} at 1×",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onSpeedClick) {
                Text(
                    text = if (playbackSpeed == 1f) "1×" else "${playbackSpeed}×",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (playbackSpeed != 1f)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSleepTimerClick) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (sleepTimerLabel.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = sleepTimerLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Sleep",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            TextButton(onClick = { showNoteDialog = true }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Note",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showNoteDialog) {
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