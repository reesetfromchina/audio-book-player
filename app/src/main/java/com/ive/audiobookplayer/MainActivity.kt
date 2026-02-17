package com.ive.audiobookplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ive.audiobookplayer.ui.screens.MainScreen
import com.ive.audiobookplayer.viewmodel.AudioViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: AudioViewModel = viewModel()

            MainScreen(
                viewModel = viewModel,
                onFolderPicked = { uris -> viewModel.playPlaylist(uris) })
        }
    }
}


