package com.example.snustracker

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

val NothingRed = Color(0xFFD71921)

// Wir nutzen die Material-Farbrollen für unsere eigenen Zwecke:
//   background        = Hintergrund
//   surface           = Karten
//   onBackground      = Text / Vordergrund
//   onSurfaceVariant  = gedämpfter Text
//   outline           = dünne Linien
//   surfaceVariant    = ausgeschaltete Punkte / leere Flächen
//   primary           = Akzentfarbe (Nothing-Rot)
private val DarkColors = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF8C8C8C),
    outline = Color(0xFF2B2B2B),
    surfaceVariant = Color(0xFF2F2F2F),
    primary = NothingRed,
    onPrimary = Color.White,
)

private val LightColors = lightColorScheme(
    background = Color(0xFFEFEFEF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    onSurfaceVariant = Color(0xFF6C6C6C),
    outline = Color(0xFFD8D8D8),
    surfaceVariant = Color(0xFFD0D0D0),
    primary = NothingRed,
    onPrimary = Color.White,
)

/** Die Dot-Matrix-Schrift (liegt in res/font/doto.ttf). */
@OptIn(ExperimentalTextApi::class)
val Doto = FontFamily(
    Font(R.font.doto, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.doto, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

@Composable
fun SnusTheme(content: @Composable () -> Unit) {
    val colors: ColorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
