package com.ive.audiobookplayer.ui.theme


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
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
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}