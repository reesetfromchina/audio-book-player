package com.ive.audiobookplayer.viewmodel

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.ive.audiobookplayer.model.SettingsState
import com.ive.audiobookplayer.repository.SettingsRepository
import com.ive.audiobookplayer.repository.ThemeMode
import com.ive.audiobookplayer.service.VolumeBoostAudioProcessor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    val settingsState: StateFlow<SettingsState> = combine(
        settingsRepository.skipForwardSeconds,
        settingsRepository.skipBackSeconds,
        settingsRepository.defaultPlaybackSpeed,
        settingsRepository.autoRewindSeconds,
        settingsRepository.finishThresholdPercent,
        settingsRepository.themeMode,
        settingsRepository.autoRescanOnOpen,
        settingsRepository.volumeBoost,
    ){
        values ->
        SettingsState(
            skipForwardSeconds = values[0] as Int,
            skipBackwardSeconds = values[1] as Int,
            defaultPlaybackSpeed = values[2] as Float,
            autoRewindSeconds = values[3] as Int,
            finishThresholdPercent = values[4] as Int,
            themeMode = values[5] as ThemeMode,
            autoRescanOnOpen = values[6] as Boolean,
            volumeBoost = values[7] as Float,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())


    fun setSkipForwardSeconds(seconds: Int) = viewModelScope.launch {
        settingsRepository.setSkipForwardSeconds(seconds)
    }
    fun setSkipBackSeconds(seconds: Int) = viewModelScope.launch {
        settingsRepository.setSkipBackSeconds(seconds)
    }
    fun setDefaultPlaybackSpeed(speed: Float) = viewModelScope.launch {
        settingsRepository.setDefaultPlaybackSpeed(speed)
    }
    fun setAutoRewindSeconds(seconds: Int) = viewModelScope.launch {
        settingsRepository.setAutoRewindSeconds(seconds)
    }
    fun setFinishThresholdPercent(percent: Int) = viewModelScope.launch {
        settingsRepository.setFinishThresholdPercent(percent)
    }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }
    fun setAutoRescanOnOpen(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setAutoRescanOnOpen(enabled)
    }

    @OptIn(UnstableApi::class)
    fun setVolumeBoost(boost: Float) = viewModelScope.launch {
        settingsRepository.setVolumeBoost(boost)
        VolumeBoostAudioProcessor.setGain(boost)
    }
}