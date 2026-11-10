package com.calias.tv.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.Text
import androidx.tv.material3.Surface

/**
 * The one glass panel: translucent fill, hairline edge, no focus zoom. Focus brightens the fill and
 * strengthens the edge. Every tile, card, row and button in the app is built on this. [colors] are the
 * theme's, except for rows over the video ([Glass.overVideo]).
 */
@Composable
fun GlassSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = Corner.Large,
    colors: GlassColors = Glass.colors,
    content: @Composable BoxScope.() -> Unit,
) {
    val text = colors.text
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.fill,
            contentColor = text,
            focusedContainerColor = colors.fillFocused,
            focusedContentColor = text,
            pressedContainerColor = colors.fillFocused,
            pressedContentColor = text,
            disabledContainerColor = colors.fill,
            disabledContentColor = text,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, colors.edge), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, colors.edgeFocused), shape = shape),
        ),
        content = content,
    )
}

/** A glass button with a text. [textStyle] is the text's look (Settings shows each font in itself). */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: GlassColors = Glass.colors,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    // A fixed height, so every button is as tall as the others whatever glyphs (a ✓) its text has.
    GlassSurface(onClick = onClick, modifier = modifier.height(Space.Control), shape = Corner.Medium, colors = colors) {
        Text(text = text, style = textStyle, modifier = Modifier.align(Alignment.Center).padding(horizontal = Space.Inner))
    }
}

/** A round glass button with one icon. [label] is the spoken description. */
@Composable
fun GlassIconButton(iconRes: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassSurface(onClick = onClick, modifier = modifier.size(52.dp), shape = CircleShape) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            modifier = Modifier.size(24.dp).align(Alignment.Center),
        )
    }
}

/**
 * A glass container that is not interactive and never takes focus: a card, a badge. The defaults are
 * the dark panel used over the video; pass the theme's [Glass.colors] fill and edge elsewhere.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = Corner.Large,
    fill: Color = Color.Black.copy(alpha = 0.90f),
    edge: Color = Color.White.copy(alpha = 0.14f),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape),
        content = content,
    )
}
