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

/**
 * The glass look as design tokens. Every screen takes its glass from here (through [Glass] and
 * GlassSurface), so the whole app is restyled by changing this file. Fills are translucent on
 * purpose: they show the backdrop through them. No real blur: it is costly on the TV, and it
 * cannot reach behind the video anyway.
 */
@Immutable
data class GlassColors(
    val fill: Color,
    val fillFocused: Color,
    val edge: Color,
    val edgeFocused: Color,
    val accent: Color,
    val text: Color,
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
    text = Color.White,
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
    text = Color(0xFF10131A),
    textMuted = Color.Black.copy(alpha = 0.60f),
    backgroundTop = Color(0xFFF4F6FA),
    backgroundBottom = Color(0xFFDDE3EE),
)

/**
 * The margins every screen keeps from the screen edge, so content stays inside the TV's safe area
 * (about 5%: 48 dp at the sides, 27 dp top and bottom) and all screens line up.
 */
object Edge {
    val X = 48.dp
    val Y = 32.dp
}

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

    /**
     * Rows drawn on the glass panels over the video: always light on dark, in either theme, with no
     * fill or edge at rest (the panel is the glass), and a bright fill when focused.
     */
    val overVideo = DarkGlass.copy(
        fill = Color.Transparent,
        fillFocused = Color.White.copy(alpha = 0.22f),
        edge = Color.Transparent,
    )
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

/**
 * The app theme: the glass tokens, the default text color, and the font. All colors come from
 * [Glass], so Material's own color scheme is left at its default and is not used.
 */
@Composable
fun AppTheme(dark: Boolean, font: AppFont, content: @Composable () -> Unit) {
    val glass = if (dark) DarkGlass else LightGlass
    CompositionLocalProvider(LocalGlass provides glass, LocalContentColor provides glass.text) {
        MaterialTheme(typography = Typography().withFamily(font.family), content = content)
    }
}
