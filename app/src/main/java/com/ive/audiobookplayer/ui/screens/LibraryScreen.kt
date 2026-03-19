package com.ive.audiobookplayer.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.model.LibraryGroups
import com.ive.audiobookplayer.utils.calculateProgress
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun LibraryScreen(
    viewModel: AudioViewModel,
    audiobooks: List<Audiobook>,
    bookmarks: Map<String, BookmarkEntry>,
    onAudiobookSelected: (Audiobook) -> Unit,
    onPickFolder: () -> Unit,
    isLibraryLoaded: Boolean,
    isScanning: Boolean,
    isBackgroundScanning: Boolean,
    libraryGroups: LibraryGroups,
) {
    val coverCache by viewModel.coverCache.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { it.key as? String }
                .filter { !it.startsWith("header_") && !it.startsWith("spacer_") }
        }
            .distinctUntilChanged()
            .collect { visibleIds ->
                if (visibleIds.isNotEmpty()) {
                    viewModel.onBooksVisible(visibleIds)
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(16.dp)
    ) {

        when {

            isScanning -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Scanning library...")
                    }
                }
            }
            !isLibraryLoaded || audiobooks.isEmpty() -> {
                // first launch OR empty library — always show the picker
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "No audiobooks found",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Pick a folder containing your audiobooks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onPickFolder) {
                            Text("Pick folder")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (libraryGroups.inProgress.isNotEmpty()) {
                        item(key = "header_inprogress") {  // unique key for header
                            LibrarySectionHeader(
                                title = "In Progress",
                                count = libraryGroups.inProgress.size
                            )
                        }
                        items(
                            items = libraryGroups.inProgress,
                            key = { it.id }  // book ID as key
                        ) { book ->
                            AudiobookCard(
                                audiobook = book,
                                bookmark = bookmarks[book.id],
                                coverBitmap = coverCache[book.id],
                                onClick = { onAudiobookSelected(book) }
                            )
                        }
                        item(key = "spacer_inprogress") {  // ✅ Unique key for spacer
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (libraryGroups.unplayed.isNotEmpty()) {
                        item(key = "header_unplayed") {
                            LibrarySectionHeader(
                                title = "Unplayed",
                                count = libraryGroups.unplayed.size
                            )
                        }
                        items(
                            items = libraryGroups.unplayed,
                            key = { it.id }
                        ) { book ->
                            AudiobookCard(
                                audiobook = book,
                                bookmark = bookmarks[book.id],
                                coverBitmap = coverCache[book.id],
                                onClick = { onAudiobookSelected(book) }
                            )
                        }
                        item(key = "spacer_unplayed") {
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (libraryGroups.finished.isNotEmpty()) {
                        item(key = "header_finished") {
                            LibrarySectionHeader(
                                title = "Finished",
                                count = libraryGroups.finished.size
                            )
                        }
                        items(
                            items = libraryGroups.finished,
                            key = { it.id }
                        ) { book ->
                            // ✅ NO LaunchedEffect here!
                            AudiobookCard(
                                audiobook = book,
                                bookmark = bookmarks[book.id],
                                coverBitmap = coverCache[book.id],
                                isFinished = true,
                                onClick = { onAudiobookSelected(book) }
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun LibrarySectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
    Spacer(Modifier.height(4.dp))
}

@Composable
fun AudiobookCard(
    audiobook: Audiobook,
    bookmark: BookmarkEntry?,
    coverBitmap: Bitmap? = null,
    isFinished: Boolean = false,
    onClick: () -> Unit
) {
    val progress = remember(bookmark) { calculateProgress(audiobook, bookmark) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isFinished)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
                if (isFinished) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Finished",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    audiobook.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFinished)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "${audiobook.chapters.size} chapter(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (bookmark != null) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (isFinished)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val minutes = (bookmark.positionMs / 1000) / 60
                        val seconds = (bookmark.positionMs / 1000) % 60
                        Text(
                            if (isFinished) "Finished"
                            else "Ch.${bookmark.chapterIndex + 1} — %02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "%.0f%%".format(progress),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isFinished)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
