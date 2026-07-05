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
    primary = StudioBlue,
    onPrimary = StudioOnAccent,
    primaryContainer = StudioBlueDark,
    onPrimaryContainer = StudioOnAccent,
    secondary = StudioTeal,
    onSecondary = StudioOnAccent,
    tertiary = StudioAmber,
    onTertiary = StudioOnAccent,
    background = StudioBackground,
    onBackground = StudioText,
    surface = StudioSurface,
    onSurface = StudioText,
    surfaceVariant = StudioPanel,
    onSurfaceVariant = StudioMutedText,
    outline = StudioBorder,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = StudioBlueLight,
    onPrimary = StudioOnAccent,
    primaryContainer = StudioBlueSoft,
    onPrimaryContainer = StudioTextLight,
    secondary = StudioTealLight,
    tertiary = StudioAmberLight,
    background = StudioLightBackground,
    onBackground = StudioTextLight,
    surface = StudioLightSurface,
    onSurface = StudioTextLight,
    surfaceVariant = StudioLightPanel,
    onSurfaceVariant = StudioMutedTextLight,
    outline = StudioLightBorder,

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

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
