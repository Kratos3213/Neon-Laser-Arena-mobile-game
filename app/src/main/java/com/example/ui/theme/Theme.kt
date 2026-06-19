package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonCyan,
    secondary = NeonMagenta,
    tertiary = NeonYellow,
    background = CosmicDark,
    surface = SpaceCard,
    onPrimary = CosmicDark,
    onSecondary = CosmicDark,
    onTertiary = CosmicDark,
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = NebulaTerminal,
    onSurfaceVariant = TextWhite
  )

private val LightColorScheme = DarkColorScheme // Force dark theme for scifi laser game

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark Cyberpunk for immersive lasers
  dynamicColor: Boolean = false, // Disable dynamic colors to keep neon design
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
