package com.ive.audiobookplayer.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ive.audiobookplayer.model.PlayerState
import com.ive.audiobookplayer.service.MediaControllerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val controllerManager = MediaControllerManager(application)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        viewModelScope.launch {
            controllerManager.controller.collect { controller ->
                _playerState.update {
                    it.copy(
                        controller = controller
                    )
                }
            }
        }
    }

    fun playPlaylist(uris: List<Uri>) {
        controllerManager.playPlaylist(uris)
    }

    fun updatePlayerState(
        isPlaying: Boolean? = null,
        currentTitle: String? = null,
        currentPosition: Long? = null,
        duration: Long? = null,
        playbackSpeed: Float? = null
    ) {
        _playerState.update { currentState ->
            currentState.copy(
                isPlaying = isPlaying ?: currentState.isPlaying,
                currentTitle = currentTitle ?: currentState.currentTitle,
                currentPosition = currentPosition ?: currentState.currentPosition,
                duration = duration ?: currentState.duration,
                playbackSpeed = playbackSpeed ?: currentState.playbackSpeed,
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        controllerManager.release()
    }
}