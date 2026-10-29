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

private val DarkColorScheme = darkColorScheme(
    primary = SolarAmberLight,
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = SolarGoldContainer,
    secondary = ElectricTealLight,
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF075985),
    onSecondaryContainer = ElectricTealContainer,
    tertiary = GridEmerald,
    background = SolarDarkBackground,
    surface = SolarDarkSurface,
    surfaceVariant = SolarDarkSurfaceVariant,
    onBackground = SolarDarkOnSurface,
    onSurface = SolarDarkOnSurface,
    onSurfaceVariant = SolarDarkOnSurfaceVariant,
    outline = SolarDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = SolarAmber,
    onPrimary = Color.White,
    primaryContainer = SolarGoldContainer,
    onPrimaryContainer = OnSolarGoldContainer,
    secondary = ElectricTeal,
    onSecondary = Color.White,
    secondaryContainer = ElectricTealContainer,
    onSecondaryContainer = OnElectricTealContainer,
    tertiary = GridEmerald,
    background = SolarLightBackground,
    surface = SolarLightSurface,
    surfaceVariant = SolarLightSurfaceVariant,
    onBackground = SolarLightOnSurface,
    onSurface = SolarLightOnSurface,
    onSurfaceVariant = SolarLightOnSurfaceVariant,
    outline = SolarLightOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep custom solar theme as primary identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
