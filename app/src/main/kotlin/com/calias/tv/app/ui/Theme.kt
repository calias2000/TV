package com.calias.tv.app.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/**
 * The glass look as design tokens. Every screen takes its glass from here (through [Glass] and
 * GlassSurface), so the whole app is restyled by changing this file. Fills are translucent on
 * purpose: they show the backdrop through them. No real blur: it is costly on the TV, and it
 * cannot reach behind the video anyway.
 */
@Immutable
class GlassColors(
    val fill: Color,
    val fillFocused: Color,
    val edge: Color,
    val edgeFocused: Color,
    val accent: Color,
    val textMuted: Color,
    val backgroundTop: Color,
    val backgroundBottom: Color,
)

private val DarkGlass = GlassColors(
    fill = Color.White.copy(alpha = 0.08f),
    fillFocused = Color.White.copy(alpha = 0.20f),
    edge = Color.White.copy(alpha = 0.14f),
    edgeFocused = Color.White.copy(alpha = 0.70f),
    accent = Color(0xFF6EC1FF),
    textMuted = Color.White.copy(alpha = 0.65f),
    backgroundTop = Color(0xFF161A22),
    backgroundBottom = Color(0xFF090B10),
)

// "Frosted light": pale background, white glass with a faint dark edge.
private val LightGlass = GlassColors(
    fill = Color.White.copy(alpha = 0.55f),
    fillFocused = Color.White.copy(alpha = 0.95f),
    edge = Color.Black.copy(alpha = 0.08f),
    edgeFocused = Color(0xFF1565C0),
    accent = Color(0xFF1E88E5), // also drawn over the dark scrim on video, so not too dark
    textMuted = Color.Black.copy(alpha = 0.60f),
    backgroundTop = Color(0xFFF4F6FA),
    backgroundBottom = Color(0xFFDDE3EE),
)

/** The backdrop color of each Home section. Our own picks: change them here. */
object SectionTint {
    val Live = Color(0xFF2E7BFF)
    val Movies = Color(0xFFFF7A45)
    val Series = Color(0xFF9B5CFF)
}

private val LocalGlass = staticCompositionLocalOf { DarkGlass }

/** Access to the current glass tokens: `Glass.colors`, `Glass.shape`. */
object Glass {
    val colors: GlassColors
        @Composable @ReadOnlyComposable get() = LocalGlass.current

    val shape = RoundedCornerShape(20.dp)
}

/** The same type scale, set in [family]. */
private fun Typography.withFamily(family: FontFamily) = copy(
    displayLarge = displayLarge.copy(fontFamily = family),
    displayMedium = displayMedium.copy(fontFamily = family),
    displaySmall = displaySmall.copy(fontFamily = family),
    headlineLarge = headlineLarge.copy(fontFamily = family),
    headlineMedium = headlineMedium.copy(fontFamily = family),
    headlineSmall = headlineSmall.copy(fontFamily = family),
    titleLarge = titleLarge.copy(fontFamily = family),
    titleMedium = titleMedium.copy(fontFamily = family),
    titleSmall = titleSmall.copy(fontFamily = family),
    bodyLarge = bodyLarge.copy(fontFamily = family),
    bodyMedium = bodyMedium.copy(fontFamily = family),
    bodySmall = bodySmall.copy(fontFamily = family),
    labelLarge = labelLarge.copy(fontFamily = family),
    labelMedium = labelMedium.copy(fontFamily = family),
    labelSmall = labelSmall.copy(fontFamily = family),
)

/** The app theme: Material colors derived from the glass tokens, plus the tokens themselves. */
@Composable
fun AppTheme(dark: Boolean, font: AppFont, content: @Composable () -> Unit) {
    val glass = if (dark) DarkGlass else LightGlass
    val text = if (dark) Color.White else Color(0xFF10131A)
    val scheme = if (dark) {
        darkColorScheme(
            primary = glass.accent,
            onPrimary = Color(0xFF05233A),
            background = glass.backgroundBottom,
            onBackground = text,
            surface = glass.backgroundBottom,
            onSurface = text,
            surfaceVariant = glass.fill,
            onSurfaceVariant = text,
            border = glass.edgeFocused,
            borderVariant = glass.edge,
        )
    } else {
        lightColorScheme(
            primary = glass.accent,
            onPrimary = Color.White,
            background = glass.backgroundBottom,
            onBackground = text,
            surface = glass.backgroundBottom,
            onSurface = text,
            surfaceVariant = glass.fill,
            onSurfaceVariant = text,
            border = glass.edgeFocused,
            borderVariant = glass.edge,
        )
    }
    // No root Surface any more, so the default text color is set here.
    CompositionLocalProvider(LocalGlass provides glass, LocalContentColor provides text) {
        MaterialTheme(colorScheme = scheme, typography = Typography().withFamily(font.family), content = content)
    }
}
