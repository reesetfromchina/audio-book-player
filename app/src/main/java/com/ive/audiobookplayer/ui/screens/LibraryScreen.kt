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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.model.LibraryState
import com.ive.audiobookplayer.ui.componenets.AudiobookGrid
import com.ive.audiobookplayer.utils.calculateProgress
import com.ive.audiobookplayer.viewmodel.AudioViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun LibraryScreen(
    viewModel: AudioViewModel,
    onAudiobookSelected: (Audiobook) -> Unit,
    onPickFolder: () -> Unit,
    library: LibraryState,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .mapNotNull { it.key as? String }
                .filter { !it.startsWith("header_") && !it.startsWith("spacer_") }
        }
            .distinctUntilChanged()
            .collect { visibleIds ->
                if (visibleIds.isNotEmpty()) viewModel.onBooksVisible(visibleIds)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Library", style = MaterialTheme.typography.headlineSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (library.isBackgroundScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
                IconButton(onClick = { viewModel.toggleLibraryView() }) {
                    Icon(
                        imageVector = if (library.isGridView) Icons.Default.ViewList
                        else Icons.Default.GridView,
                        contentDescription = null
                    )
                }
                IconButton(onClick = onPickFolder) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Change folder")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            library.isInitializing -> {
                Box(Modifier.fillMaxSize())
            }

            library.isScanning -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Scanning library...")
                    }
                }
            }

            library.audiobooks.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("No audiobooks found", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Pick a folder containing your audiobooks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onPickFolder) { Text("Pick folder") }
                    }
                }
            }

            library.isGridView -> {
                val sortedBooks = remember(library.groups) {
                    library.groups.inProgress +
                            library.groups.unplayed +
                            library.groups.finished
                }
                AudiobookGrid(
                    audiobooks = sortedBooks,
                    bookmarks = library.bookmarks,
                    onAudiobookSelected = onAudiobookSelected,
                    viewModel = viewModel,
                    onBooksVisible = { viewModel.onBooksVisible(it) }
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    librarySection(
                        title = "In Progress",
                        books = library.groups.inProgress,
                        bookmarks = library.bookmarks,
                        viewModel = viewModel,
                        onAudiobookSelected = onAudiobookSelected,
                    )
                    librarySection(
                        title = "Unplayed",
                        books = library.groups.unplayed,
                        bookmarks = library.bookmarks,
                        viewModel = viewModel,
                        onAudiobookSelected = onAudiobookSelected,
                    )
                    librarySection(
                        title = "Finished",
                        books = library.groups.finished,
                        bookmarks = library.bookmarks,
                        viewModel = viewModel,
                        onAudiobookSelected = onAudiobookSelected,
                        isFinished = true,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.librarySection(
    title: String,
    books: List<Audiobook>,
    bookmarks: Map<String, BookmarkEntry>,
    viewModel: AudioViewModel,
    onAudiobookSelected: (Audiobook) -> Unit,
    isFinished: Boolean = false,
) {
    if (books.isEmpty()) return
    val key = title.lowercase().replace(" ", "_")

    item(key = "header_$key") {
        LibrarySectionHeader(title = title, count = books.size)
    }
    items(items = books, key = { it.id }) { book ->
        AudiobookCard(
            audiobook = book,
            bookmark = bookmarks[book.id],
            isFinished = isFinished,
            viewModel = viewModel,
            onClick = { onAudiobookSelected(book) }
        )
    }
    item(key = "spacer_$key") {
        Spacer(Modifier.height(8.dp))
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
    viewModel: AudioViewModel,
    isFinished: Boolean = false,
    onClick: () -> Unit
) {
    val progress = remember(bookmark) { calculateProgress(audiobook, bookmark) }
    val coverBitmap by remember(audiobook.id) {
        viewModel.coverCache.map { it[audiobook.id] }
    }.collectAsState(initial = null)

    val imageBitmap = remember(coverBitmap) { coverBitmap?.asImageBitmap() }
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
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
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
                            else "Ch.${bookmark.chapterIndex + 1} — %02d:%02d".format(
                                minutes,
                                seconds
                            ),
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
