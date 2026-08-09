package com.ive.audiobookplayer.repository
import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map


enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val ROOT_FOLDER_URI_KEY = stringPreferencesKey("root_folder_uri")
    private val LAST_BOOK_ID_KEY = stringPreferencesKey("last_book_id")
    private val SKIP_FORWARD_SECODNS_KEY = intPreferencesKey("skip_forward_seconds")
    private val SKIP_BACK_SECONDS_KEY = intPreferencesKey("skip_back_seconds")
    private val DEFAULT_PLAYBACK_SPEED_KEY = floatPreferencesKey("default_playback_speed")
    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
    private val AUTO_RESCAN_ON_OPEN_KEY = booleanPreferencesKey("auto_rescan_op_open")
    private val AUTO_REWIND_SECONDS_KEY = intPreferencesKey("auto_rewind_seconds")
    private val FINISH_THRESHOLD_PERCENT_KEY = intPreferencesKey("finish_threshold_percent")
    private val VOLUME_BOOST_KEY = floatPreferencesKey("volume_boost")

    val rootFolderUri: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[ROOT_FOLDER_URI_KEY]
    }

    suspend fun saveRootFolderUri(uri: Uri) {
        context.settingsDataStore.edit { prefs ->
            prefs[ROOT_FOLDER_URI_KEY] = uri.toString()
        }
    }

    suspend fun getLastBookId(): String? =
        context.settingsDataStore.data.first()[LAST_BOOK_ID_KEY]

    suspend fun saveLastBookId(id: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[LAST_BOOK_ID_KEY] = id
        }
    }

    suspend fun clearLastBookId() {
        context.settingsDataStore.edit { prefs ->
            context.settingsDataStore.edit { it.remove(LAST_BOOK_ID_KEY) }
        }
    }


    //   playback
    val skipForwardSeconds: Flow<Int> =
        context.settingsDataStore.data.map { it[SKIP_FORWARD_SECODNS_KEY] ?: 30 }

    suspend fun setSkipForwardSeconds(seconds: Int){
        context.settingsDataStore.edit { it[SKIP_FORWARD_SECODNS_KEY] = seconds }
    }

    val skipBackSeconds: Flow<Int> =
        context.settingsDataStore.data.map { it[SKIP_BACK_SECONDS_KEY] ?: 10 }

    suspend fun setSkipBackSeconds(seconds: Int){
        context.settingsDataStore.edit { it[SKIP_BACK_SECONDS_KEY] = seconds }
    }

    val defaultPlaybackSpeed: Flow<Float> =
        context.settingsDataStore.data.map { it[DEFAULT_PLAYBACK_SPEED_KEY] ?: 1f}

    suspend fun setDefaultPlaybackSpeed(speed : Float){
        context.settingsDataStore.edit { it[DEFAULT_PLAYBACK_SPEED_KEY] = speed }
    }


    // after pause auto rewind
    val autoRewindSeconds: Flow<Int> =
        context.settingsDataStore.data.map { it[AUTO_REWIND_SECONDS_KEY] ?: 0 }

    suspend fun setAutoRewindSeconds(seconds: Int){
        context.settingsDataStore.edit { it[AUTO_REWIND_SECONDS_KEY] = seconds }
    }

    val finishThresholdPercent: Flow<Int> =
        context.settingsDataStore.data.map { it[FINISH_THRESHOLD_PERCENT_KEY] ?: 100 }

    suspend fun setFinishThresholdPercent(percent : Int){
        context.settingsDataStore.edit { it[FINISH_THRESHOLD_PERCENT_KEY] = percent }
    }

    val volumeBoost: Flow<Float> =
        context.settingsDataStore.data.map { it[VOLUME_BOOST_KEY] ?: 1f }

    suspend fun setVolumeBoost(boost: Float) {
        context.settingsDataStore.edit { it[VOLUME_BOOST_KEY] = boost }
    }



//    appearance

    val themeMode : Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        ThemeMode.entries.find {it.name == prefs[THEME_MODE_KEY]} ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode){
        context.settingsDataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    // library

    val autoRescanOnOpen: Flow<Boolean> =
        context.settingsDataStore.data.map { it[AUTO_RESCAN_ON_OPEN_KEY] ?: true }

    suspend fun setAutoRescanOnOpen(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_RESCAN_ON_OPEN_KEY] = enabled }
    }
}
