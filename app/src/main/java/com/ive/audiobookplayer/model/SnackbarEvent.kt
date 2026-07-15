package com.ive.audiobookplayer.model

data class SnackbarEvent(
    val message: String,
    val hasViewNotesAction: Boolean = false,
    val id: Long = System.currentTimeMillis()
)
