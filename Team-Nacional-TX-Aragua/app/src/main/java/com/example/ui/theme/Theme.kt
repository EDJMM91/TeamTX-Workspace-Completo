package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.preferences.PreferenciasApp

private val DarkColorScheme = darkColorScheme(
    primary = MotoOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = MotoOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = MotoGoldSecondary,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4A3400),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = StatusInfo,
    onTertiary = Color.White,
    background = AsphaltDarkBackground,
    onBackground = Color(0xFFE2E6EE),
    surface = AsphaltDarkSurface,
    onSurface = Color(0xFFE2E6EE),
    surfaceVariant = AsphaltDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC0C7D5),
    outline = AsphaltDarkBorder,
    error = StatusError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = MotoOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF380D00),
    secondary = Color(0xFF8A5100),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB8),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Color(0xFF006688),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFC7CDD8),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = PreferenciasApp.modoOscuroState.value,
    dynamicColor: Boolean = false,
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
        typography = Typography,
        content = content
    )
}
