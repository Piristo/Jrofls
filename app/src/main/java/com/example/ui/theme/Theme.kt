package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CraftPalette(
  val paperTint: Color = CraftPaperTint,
  val brassEyelet: Color = CraftBrassDot,
  val headerGlow: Color = SoftMintContainer,
)

val LocalCraftPalette = staticCompositionLocalOf { CraftPalette() }

private val WarmLightColorScheme =
  lightColorScheme(
    primary = ForestGreen,
    onPrimary = Color.White,
    primaryContainer = SoftMintContainer,
    onPrimaryContainer = ForestGreenDark,
    secondary = ForestGreen,
    onSecondary = Color.White,
    secondaryContainer = SoftMintContainer,
    onSecondaryContainer = ForestGreenDark,
    background = WarmCreamBackground,
    onBackground = InkPrimary,
    surface = WarmSurface,
    onSurface = InkPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLow = WarmSurface,
    surfaceContainer = WarmSurfaceVariant,
    outline = WarmOutline,
    outlineVariant = WarmOutline,
    error = TerracottaError,
    onError = Color.White,
    errorContainer = TerracottaContainer,
    onErrorContainer = TerracottaError,
  )

val AppShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun GdeLezhitTheme(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalCraftPalette provides CraftPalette()) {
    MaterialTheme(
      colorScheme = WarmLightColorScheme,
      typography = Typography,
      shapes = AppShapes,
      content = content,
    )
  }
}
