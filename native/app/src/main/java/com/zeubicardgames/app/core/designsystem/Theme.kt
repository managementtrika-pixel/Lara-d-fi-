package com.zeubicardgames.app.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ZeubiBackground = Color(0xFFEDF4FA)
val ZeubiSurface = Color(0xFFF8FBFE)
val ZeubiSecondary = Color(0xFFE6EEF6)
val ZeubiInk = Color(0xFF222831)
val ZeubiMuted = Color(0xFF788697)
val ZeubiViolet = Color(0xFF7657FF)
val ZeubiGold = Color(0xFFD8B44C)

private val Scheme = lightColorScheme(primary = ZeubiViolet, secondary = ZeubiGold, background = ZeubiBackground, surface = ZeubiSurface, onBackground = ZeubiInk, onSurface = ZeubiInk, surfaceVariant = ZeubiSecondary, onSurfaceVariant = ZeubiMuted)
@Composable fun ZeubiTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
