package com.calias.tv.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** The subtitle sizes the user can pick in Settings, in sp. */
enum class SubtitleSize(val label: String, val sp: Int) {
    SMALL("Small", 20),
    MEDIUM("Medium", 24),
    LARGE("Large", 30),
    EXTRA_LARGE("Extra large", 36);

    companion object {
        val DEFAULT = MEDIUM
    }
}

/**
 * One subtitle line on dark glass, in the app font at [size]. The player draws it over the video and
 * Settings previews it, so what you pick is what you get. White on dark: it sits on video in either theme.
 */
@Composable
fun SubtitleLine(text: String, size: SubtitleSize, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = size.sp.sp, lineHeight = (size.sp * 1.3f).sp),
        textAlign = TextAlign.Center,
        modifier = modifier
            .widthIn(max = 800.dp)
            .background(Color.Black.copy(alpha = 0.6f), Corner.Small)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
