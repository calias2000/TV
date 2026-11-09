package com.calias.tv.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.calias.tv.core.Channel
import com.calias.tv.core.ContentKind

/**
 * The channel logo, or the movie poster or episode still. Live logos and episode stills are wide,
 * movie posters are tall, so each gets its own box. Live logos are mostly transparent PNGs made for
 * a light background, so they sit on a small light chip and stay readable on dark glass. Entries
 * without a logo keep an empty box, so the names still line up.
 * Coil loads and caches the image (memory and disk), and downsamples it to the box size.
 */
@Composable
fun ChannelLogo(channel: Channel, modifier: Modifier = Modifier) {
    val box = if (channel.kind == ContentKind.MOVIE) Modifier.size(44.dp, 66.dp) else Modifier.size(64.dp, 36.dp)
    val chip = channel.kind == ContentKind.LIVE && !channel.logo.isNullOrBlank()
    val background = if (chip) Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.92f)) else Modifier
    Box(modifier = modifier.then(box).then(background), contentAlignment = Alignment.Center) {
        if (!channel.logo.isNullOrBlank()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = null, // decorative: the name is next to it
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(if (chip) 4.dp else 0.dp),
            )
        }
    }
}
