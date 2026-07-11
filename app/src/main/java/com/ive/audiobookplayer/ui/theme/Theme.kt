package com.ive.audiobookplayer.ui.theme


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = Color(0xFFB5651D),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF3B1B00),

    secondary = Color(0xFF8D6E63),
    onSecondary = Color(0xFFFFFFFF),

    background = Color(0xFFFFF8F2),
    onBackground = Color(0xFF2A1D14),

    surface = Color(0xFFFFF1E6),
    onSurface = Color(0xFF2A1D14),
    onSurfaceVariant = Color(0xFF5C4A3D),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB77C),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6B3E00),
    onPrimaryContainer = Color(0xFFFFDCC2),

    secondary = Color(0xFFD7B8A9),
    onSecondary = Color(0xFF3E2A1F),

    background = Color(0xFF1C1512),
    onBackground = Color(0xFFEDE0D5),

    surface = Color(0xFF241B16),
    onSurface = Color(0xFFEDE0D5),
    onSurfaceVariant = Color(0xFFCBB6A5),
)

private val DarkScheme = darkColorScheme(
    primary = red,
    onPrimary = white,
    background = black,
    surface = darkGrey,
    onBackground = white,
    onSurface = white,
    secondaryContainer = Color(0xFF212121)
)

private val LightScheme = lightColorScheme(
    primary = red,
    onPrimary = white,
    background = white,
    surface = lightGrey,
    onBackground = black,
    onSurface = black,
    secondaryContainer = Color(0xFFE5E5E5)
)


private val DarkAudiobookScheme = darkColorScheme(
    primary = BookAmber,
    onPrimary = Color.Black,
    background = DeepEspresso,
    surface = Color(0xFF252121),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    secondaryContainer = Color(0xFF3E3939)
)

private val LightAudiobookScheme = lightColorScheme(
    primary = BookAmber,
    onPrimary = Color.Black,
    background = AgedPaper,
    surface = Color(0xFFF5EEDA),
    onBackground = InkBlack,
    onSurface = InkBlack,
    secondaryContainer = Color(0xFFE9E0C9)
)

@Composable
fun AudioBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors


    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}