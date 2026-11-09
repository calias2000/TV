package com.calias.tv.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.calias.tv.core.color.Dominant

/** The color that tints the background right now (null: none). Screens set it on focus. */
@Stable
class BackdropState {
    var color by mutableStateOf<Color?>(null)
}

val LocalBackdrop = staticCompositionLocalOf { BackdropState() }

/** How strong the colored glow is. Low: it is a mood, never a picture. */
private const val GLOW_ALPHA = 0.38f

/** Pictures are read at this size to find their color: tiny is enough and cheap. */
private const val SAMPLE_PX = 32

/**
 * The background of every screen: a soft vertical gradient with, optionally, a large soft glow of one
 * color (a section's color, or the main color of the focused picture). Only gradients are drawn, no
 * blur effect and no picture, so it costs almost nothing on the TV.
 */
@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val backdrop = remember { BackdropState() }
    val glass = Glass.colors
    val none = glass.backgroundTop.copy(alpha = 0f)
    val glow by animateColorAsState(
        targetValue = backdrop.color?.copy(alpha = GLOW_ALPHA) ?: none,
        label = "backdrop glow",
    )
    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(glass.backgroundTop, glass.backgroundBottom)))
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(glow, glow.copy(alpha = 0f)),
                            center = Offset(size.width * 0.5f, size.height * 0.3f),
                            radius = size.width * 0.75f,
                        ),
                    )
                },
        ) {
            content()
        }
    }
}

/**
 * While [focused], tints the background with the main color of the picture at [url], or [fallback]
 * when it has none. The color arrives a moment after the card is first focused, so it is applied when
 * it is ready, not only when focus changes. The picture is only read for the focused card.
 */
@Composable
fun PictureGlow(focused: Boolean, url: String?, fallback: Color) {
    val backdrop = LocalBackdrop.current
    val tint = if (focused) rememberPictureColor(url) else null
    LaunchedEffect(focused, tint) { if (focused) backdrop.color = tint ?: fallback }
}

/** The main color of the picture at [url], once loaded (null while loading, or if it has no clear color). */
@Composable
private fun rememberPictureColor(url: String?): Color? {
    val context = LocalContext.current
    val color by produceState<Color?>(initialValue = null, url) {
        value = null
        if (url.isNullOrBlank()) return@produceState
        val request = ImageRequest.Builder(context).data(url).size(SAMPLE_PX).allowHardware(false).build()
        val bitmap = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
            ?: return@produceState
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        value = Dominant.color(pixels)?.let { Color(it) }
    }
    return color
}
