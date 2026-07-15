package com.ive.audiobookplayer.ui.componenets.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ive.audiobookplayer.repository.ThemeMode
import com.ive.audiobookplayer.ui.componenets.SpeedPickerDialog
import com.ive.audiobookplayer.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by settingsViewModel.settingsState.collectAsState()

    var showSkipForwardDialog by remember { mutableStateOf(false) }
    var showSkipBackDialog by remember { mutableStateOf(false) }
    var showDefaultSpeedDialog by remember { mutableStateOf(false) }
    var showAutoRewindDialog by remember { mutableStateOf(false) }
    var showFinishThresholdDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) {
        padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
        ) {
            SettingsSection(title = "Playback"){
                SettingsRow(
                    label = "Skip forward",
                    value = "${state.skipForwardSeconds}",
                    onClick = {showSkipForwardDialog = true}
                )
                SettingsRow(
                    label = "Skip backward",
                    value = "${state.skipBackwardSeconds}",
                    onClick = {showSkipBackDialog = true}
                )
                SettingsRow(
                    label = "Default playback speed",
                    value = "${state.defaultPlaybackSpeed}",
                    onClick = {showDefaultSpeedDialog = true}
                )

                SettingsRow(
                    label = "Mark finished at",
                    value = "${state.finishThresholdPercent}%",
                    onClick = {showFinishThresholdDialog = true}
                )

                SettingsRow(
                    label = "Auto rewind on resume",
                    value = if (state.autoRewindSeconds == 0) "Off" else "${state.autoRewindSeconds}s",
                    onClick = {showAutoRewindDialog = true}
                )
            }

            SettingsSection(title = "Appearance"){
                SettingsSegmentedRow(
                    label = "Theme",
                    options = ThemeMode.entries.toList(),
                    selected = state.themeMode,
                    optionLabel = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                    onSelect = { settingsViewModel.setThemeMode(it) }
                )
            }

            SettingsSection(title = "Library") {
                SettingsSwitchRow(
                    label = "Rescan library on open",
                    checked = state.autoRescanOnOpen,
                    onCheckedChange = { settingsViewModel.setAutoRescanOnOpen(it) }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if(showSkipForwardDialog){
        SecondsPickerDialog(
            title = "Skip forward interval",
            options = listOf(10,15,30,45,60),
            current = state.skipForwardSeconds,
            onSelect = { settingsViewModel.setSkipForwardSeconds(it)},
            onDismiss = { showSkipForwardDialog = false}
        )
    }

    if (showSkipBackDialog) {
        SecondsPickerDialog(
            title = "Skip back interval",
            options = listOf(10, 15, 30, 45, 60),
            current = state.skipBackwardSeconds,
            onSelect = { settingsViewModel.setSkipBackSeconds(it) },
            onDismiss = { showSkipBackDialog = false }
        )
    }
    if (showDefaultSpeedDialog) {
        SpeedPickerDialog(
            currentSpeed = state.defaultPlaybackSpeed,
            onSpeedSelected = { settingsViewModel.setDefaultPlaybackSpeed(it) },
            onDismiss = { showDefaultSpeedDialog = false }
        )
    }

    if (showAutoRewindDialog) {
        SecondsPickerDialog(
            title = "Auto-rewind on resume",
            options = listOf(0, 5, 10, 15, 30),
            current = state.autoRewindSeconds,
            optionLabel = { if (it == 0) "Off" else "${it}s" },
            onSelect = { settingsViewModel.setAutoRewindSeconds(it) },
            onDismiss = { showAutoRewindDialog = false }
        )
    }
    if (showFinishThresholdDialog) {
        SecondsPickerDialog(
            title = "Mark finished at",
            options = listOf(90, 95, 98, 100),
            current = state.finishThresholdPercent,
            optionLabel = { "$it%" },
            onSelect = { settingsViewModel.setFinishThresholdPercent(it) },
            onDismiss = { showFinishThresholdDialog = false }
        )
    }
}