package com.ive.audiobookplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ive.audiobookplayer.R

// Set of Material typography styles to start with
//val Typography = Typography(
//    bodyLarge = TextStyle(
//        fontFamily = FontFamily.Default,
//        fontWeight = FontWeight.Normal,
//        fontSize = 16.sp,
//        lineHeight = 24.sp,
//        letterSpacing = 0.5.sp
//    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
//)


val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val PlayfairFont = GoogleFont("Playfair Display")
val InterFont = GoogleFont("Inter")

val BookTitleFontFamily = FontFamily(
    Font(googleFont = PlayfairFont, fontProvider = provider)
)

val BodyFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = provider)
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = BookTitleFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = BookTitleFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontSize = 16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)