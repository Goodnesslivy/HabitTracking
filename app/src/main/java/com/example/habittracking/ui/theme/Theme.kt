package com.example.habittracking.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Standard light fallback theme colors (if dynamic theme isn't supported)
private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4C6EF5),
    secondary = androidx.compose.ui.graphics.Color(0xFF22B8CF)
)

// Standard dark fallback theme colors
private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF7950F2),
    secondary = androidx.compose.ui.graphics.Color(0xFF12B886)
)

@Composable
fun HabitTrackingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (API 31)
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
