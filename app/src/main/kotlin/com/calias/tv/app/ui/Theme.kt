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
    edgeFocused = Color.Black.copy(alpha = 0.65f),
    accent = Color(0xFF1E88E5), // also drawn over the dark scrim on video, so not too dark
    text = Color(0xFF10131A),
    textMuted = Color.Black.copy(alpha = 0.60f),
    backgroundTop = Color(0xFFF4F6FA),
    backgroundBottom = Color(0xFFDDE3EE),
)

/**
 * The margin every screen keeps from the screen edge, the same on all four sides, so content lines up
 * everywhere. If a TV cuts off the edges of the picture, raise it here and every screen follows.
 */
object Edge {
    val Margin = 32.dp
}

/** The spacing every panel, row, button and field shares, so they line up and look the same size everywhere. */
object Space {
    /** From a panel's edge to what is inside it. */
    val Panel = 32.dp

    /** Inside a button, field or row, from its edge to its content on the left and right. */
    val Inner = 24.dp

    /** Between neighbors: rows, fields, buttons. */
    val Gap = 16.dp

    /** From a side panel's edge to its title and rows. Corner.Large is Corner.Medium plus this, so the corners stay parallel. */
    val Inset = 12.dp

    /** Between the rows of a list: channels, episodes, profiles, programs. */
    val Row = 8.dp

    /** The height of a button or a text field. */
    val Control = 48.dp

    /** Room around a scrolling list for a focused row's edge, which the list would cut flat otherwise. */
    val FocusEdge = 4.dp
}

/**
 * The three corner radii of the app, so no two surfaces round differently by accident. A shape inside
 * another keeps its corners parallel to the outer ones: its radius is the outer radius minus the gap
 * between them (side panels: [Large] 24 around rows of [Medium] 12 inset by 12). Where the gap is
 * wider than the radius, as in a modal, the inner shape just takes the scale. Nothing is fully round,
 * except the icon buttons in Home's header (CircleShape in GlassIconButton) and progress bars.
 */
object Corner {
    /** What sits inside a row or card: a logo chip, the subtitle box. */
    val Small = RoundedCornerShape(8.dp)

    /** What you press or read inside a panel: buttons, fields, list rows, cards. */
    val Medium = RoundedCornerShape(12.dp)

    /** Panels, modals and the big Home tiles. */
    val Large = RoundedCornerShape(24.dp)
}

/** The backdrop color of each Home section. */
object SectionTint {
    val Live = Color(0xFF2E7BFF)
    val Movies = Color(0xFFFF7A45)
    val Series = Color(0xFF9B5CFF)
}

private val LocalGlass = staticCompositionLocalOf { DarkGlass }

/** Access to the current glass tokens: `Glass.colors`. */
object Glass {
    val colors: GlassColors
        @Composable @ReadOnlyComposable get() = LocalGlass.current

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
