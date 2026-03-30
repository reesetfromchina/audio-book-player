package com.ive.audiobookplayer.ui.componenets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.utils.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuComponent(
    onDismissRequest: () -> Unit,
    showMenu: Boolean, audiobook: Audiobook,
    bookmarkEntry: BookmarkEntry?,
    onResetProgress: () -> Unit,
    onFinishedProgress: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Reset progress") },
            onClick = {
                onResetProgress()
                onDismissRequest()
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Mark as finished") },
            onClick = {
                onFinishedProgress()
                onDismissRequest()
            }
        )

        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Book info") },
            onClick = {
                showBottomSheet = true
                onDismissRequest()
            }
        )
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(audiobook.title)
                HorizontalDivider()
                Text("${audiobook.chapters.size} chapters")
                HorizontalDivider()

                bookmarkEntry?.chapterIndex?.let {
                    if (it > 1) {
                        Text("Current chapter: ${bookmarkEntry.chapterIndex} chapter")
                    }
                }
                HorizontalDivider()

                bookmarkEntry?.positionMs?.let {
                    Text("Current position: ${TimeFormatter.formatTime(bookmarkEntry.positionMs)}")

                }
                if (audiobook.coverUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(audiobook.coverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(128.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}


