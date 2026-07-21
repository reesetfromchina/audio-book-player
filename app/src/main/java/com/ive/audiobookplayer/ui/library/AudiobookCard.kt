package com.ive.audiobookplayer.ui.library

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.ui.components.DropdownMenuComponent
import com.ive.audiobookplayer.utils.calculateProgress

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AudiobookCard(
    audiobook: Audiobook,
    bookmark: BookmarkEntry?,
    coverUri: Uri?,
    isFinished: Boolean = false,
    onClick: () -> Unit,
    onResetProgress: () -> Unit,
    onFinishedAudiobook: () -> Unit,
    onViewNote: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    val progress = remember(audiobook, bookmark) { calculateProgress(audiobook, bookmark) }
    var expanded by remember { mutableStateOf(false) }


    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        expanded = true
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    }),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFinished)
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AudiobookCover(coverUri, isFinished)

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            audiobook.title,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(
                            modifier = Modifier.width(8.dp)
                        )
                        Text(
                            "${audiobook.chapters.size} Chapters",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (bookmark != null) {
                        Spacer(Modifier.height(4.dp))
                        AudiobookProgressRow(bookmark, progress, isFinished)
                    }
                }
            }
        }
        DropdownMenuComponent(
            showMenu = expanded,
            onDismissRequest = { expanded = false },
            audiobook = audiobook,
            bookmarkEntry = bookmark,
            onResetProgress = onResetProgress,
            onFinishedProgress = onFinishedAudiobook,
            coverUri = coverUri,
            onViewNotes = onViewNote,
        )

    }
}