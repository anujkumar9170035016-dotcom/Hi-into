package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val PingBridgeLightColorScheme = lightColorScheme(
    primary = WhatsAppTeal,
    onPrimary = Color.White,
    primaryContainer = LightNavIndicator,
    onPrimaryContainer = Color(0xFF002117),
    secondary = WhatsAppEmerald,
    onSecondary = Color.White,
    secondaryContainer = LightNavIndicator,
    onSecondaryContainer = Color(0xFF002117),
    tertiary = WhatsAppReadBlue,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightDivider
)

private val PingBridgeDarkColorScheme = darkColorScheme(
    primary = WhatsAppEmerald,
    onPrimary = Color(0xFF00382B),
    primaryContainer = DarkNavIndicator,
    onPrimaryContainer = Color(0xFF7CF8D4),
    secondary = WhatsAppEmerald,
    onSecondary = Color(0xFF00382B),
    secondaryContainer = DarkNavIndicator,
    onSecondaryContainer = Color(0xFF7CF8D4),
    tertiary = WhatsAppReadBlue,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkDivider
)

@Composable
fun PingBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PingBridgeDarkColorScheme else PingBridgeLightColorScheme
    val chatPalette = if (darkTheme) DarkChatPalette else LightChatPalette

    CompositionLocalProvider(LocalChatColors provides chatPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PingBridgeTypography,
            content = content
        )
    }
}
