package com.ive.audiobookplayer.model

data class LibraryGroups(
    val inProgress: List<Audiobook> = emptyList(),
    val unplayed: List<Audiobook> = emptyList(),
    val finished: List<Audiobook> = emptyList(),
)


