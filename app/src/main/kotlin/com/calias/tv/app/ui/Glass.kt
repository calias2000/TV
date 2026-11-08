package com.calias.tv.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface

/**
 * The one glass panel: translucent fill, hairline edge, no focus zoom. Focus brightens the fill and
 * strengthens the edge. Every tile, card and button in the app is built on this.
 */
@Composable
fun GlassSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = Glass.shape,
    content: @Composable BoxScope.() -> Unit,
) {
    val glass = Glass.colors
    val text = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = glass.fill,
            contentColor = text,
            focusedContainerColor = glass.fillFocused,
            focusedContentColor = text,
            pressedContainerColor = glass.fillFocused,
            pressedContentColor = text,
            disabledContainerColor = glass.fill,
            disabledContentColor = text,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, glass.edge), shape = shape),
            focusedBorder = Border(BorderStroke(2.dp, glass.edgeFocused), shape = shape),
        ),
        content = content,
    )
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
