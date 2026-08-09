package com.ive.audiobookplayer.ui.library


import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.model.Audiobook
import com.ive.audiobookplayer.model.BookmarkEntry
import com.ive.audiobookplayer.model.LibraryGroups
import com.ive.audiobookplayer.model.LibraryState

@Composable
 fun LibraryBody(
    library: LibraryState,
    listState: LazyListState,
    resolveCover: (Audiobook) -> Uri?,
    onPickFolder: () -> Unit,
    actions: LibraryActions,
) {
    when {
        library.isInitializing -> Box(Modifier.fillMaxSize())
        library.isScanning -> ScanningIndicator()
        library.audiobooks.isEmpty() -> EmptyLibraryPlaceholder(onPickFolder = onPickFolder)
        library.isGridView -> LibraryGridBody(library, resolveCover, actions)
        else -> LibraryListBody(listState, library, resolveCover, actions)
    }
}

@Composable
private fun ScanningIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(8.dp))
            Text("Scanning library...")
        }
    }
}

@Composable
private fun EmptyLibraryPlaceholder(onPickFolder: () -> Unit) {
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

@Composable
private fun LibraryGridBody(
    library: LibraryState,
    resolveCover: (Audiobook) -> Uri?,
    actions: LibraryActions,
) {
    val sortedBooks = remember(library.groups) {
        library.groups.inProgress + library.groups.unplayed + library.groups.finished
    }
    AudiobookGrid(
        audiobooks = sortedBooks,
        bookmarks = library.bookmarks,
        resolveCover = resolveCover,
        onAudiobookSelected = actions.onAudiobookSelected,
        onResetProgress = actions.onResetProgress,
        onFinishedAudiobook = actions.onFinishedAudiobook,
        onViewNotes = actions.onViewNotes,
        onEditBook = actions.onEditBook,
    )
}

@Composable
private fun LibraryListBody(
    listState: LazyListState,
    library: LibraryState,
    resolveCover: (Audiobook) -> Uri?,
    actions: LibraryActions,
) {
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        librarySection(
            title = "In Progress",
            books = library.groups.inProgress,
            bookmarks = library.bookmarks,
            resolveCover = resolveCover,
            actions = actions,
        )
        librarySection(
            title = "Unplayed",
            books = library.groups.unplayed,
            bookmarks = library.bookmarks,
            resolveCover = resolveCover,
            actions = actions,
        )
        librarySection(
            title = "Finished",
            books = library.groups.finished,
            bookmarks = library.bookmarks,
            resolveCover = resolveCover,
            isFinished = true,
            actions = actions,
        )
    }
}

private fun LazyListScope.librarySection(
    title: String,
    books: List<Audiobook>,
    bookmarks: Map<String, BookmarkEntry>,
    resolveCover: (Audiobook) -> Uri?,
    isFinished: Boolean = false,
    actions: LibraryActions,
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
            coverUri = resolveCover(book),
            isFinished = isFinished,
            onClick = { actions.onAudiobookSelected(book) },
            onResetProgress = { actions.onResetProgress(book) },
            onFinishedAudiobook = { actions.onFinishedAudiobook(book) },
            onViewNote = { actions.onViewNotes(book) },
            onEditBook = { title, author -> actions.onEditBook(book, title, author) }
        )
    }
    item(key = "spacer_$key") {
        Spacer(Modifier.height(8.dp))
    }
}
private fun LibraryGroups.sortedForGrid(): List<Audiobook> = inProgress + unplayed + finished