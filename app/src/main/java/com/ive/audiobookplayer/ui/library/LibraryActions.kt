package com.ive.audiobookplayer.ui.library

import com.ive.audiobookplayer.model.Audiobook

data class LibraryActions(
    val onAudiobookSelected: (Audiobook) -> Unit,
    val onResetProgress: (Audiobook) -> Unit,
    val onFinishedAudiobook: (Audiobook) -> Unit,
    val onViewNotes: (Audiobook) -> Unit,
    val onEditBook: (Audiobook, String, String) -> Unit,
)