package com.ive.audiobookplayer.model

import com.ive.audiobookplayer.repository.ThemeMode

data class SettingsState(
    val skipForwardSeconds: Int = 30,
    val skipBackwardSeconds: Int = 10,
    val defaultPlaybackSpeed: Float = 1f,
    val autoRewindSeconds: Int = 0,
    val finishThresholdPercent: Int = 100,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoRescanOnOpen: Boolean = true,
    val volumeBoost: Float = 1f,
)
