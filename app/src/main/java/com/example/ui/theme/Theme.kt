package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AsvasaDarkColorScheme = darkColorScheme(
    primary = SignalCyan,
    onPrimary = TacticalDark,
    primaryContainer = TacticalSurfaceVariant,
    onPrimaryContainer = SignalCyan,
    secondary = SafetyAmber,
    onSecondary = TacticalDark,
    secondaryContainer = TacticalSurfaceVariant,
    onSecondaryContainer = SafetyAmber,
    tertiary = SafeGreen,
    onTertiary = Color.Black,
    error = EmergencyRed,
    onError = Color.White,
    background = TacticalDark,
    onBackground = TextLight,
    surface = TacticalSurface,
    onSurface = TextLight,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TextMuted,
    outline = TacticalBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to high-contrast tactical dark for emergency readability
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AsvasaDarkColorScheme,
        typography = Typography,
        content = content
    )
}
