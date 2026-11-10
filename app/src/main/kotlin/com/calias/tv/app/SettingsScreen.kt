package com.calias.tv.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassButton
import com.calias.tv.app.ui.GlassPanel
import com.calias.tv.app.ui.LocalBackdrop
import com.calias.tv.app.ui.Spinner
import com.calias.tv.app.ui.SubtitleLine
import com.calias.tv.app.ui.SubtitleSize
import com.calias.tv.app.ui.fadeEdges

/** A mid-dark "picture" behind the subtitle preview, so the dark subtitle glass shows against it. */
private val PREVIEW_PICTURE = Color(0xFF3B424D)

/**
 * Settings: glass sections for the playlist (source, status, sync), the font, and the subtitle size with
 * a live preview. The sections scroll when they do not fit the screen.
 */
@Composable
fun SettingsScreen(
    state: PlaylistState,
    onSync: () -> Unit,
    font: AppFont,
    onFont: (AppFont) -> Unit,
    subtitleSize: SubtitleSize,
    onSubtitleSize: (SubtitleSize) -> Unit,
) {
    // No glow here: the screens before it leave theirs on.
    val backdrop = LocalBackdrop.current
    LaunchedEffect(Unit) { backdrop.color = null }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = Edge.Margin).padding(top = Edge.Margin)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        // Padding inside the scroll, so the focus rings at the ends are not faded.
        Column(
            modifier = Modifier.fillMaxSize().fadeEdges().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection("Playlist") {
                Text(text = PlaylistLoader.PLAYLIST_URL, color = Glass.colors.textMuted)
                statusText(state)?.let { Text(text = it) } ?: Spinner(diameter = 24.dp)
                GlassButton(text = "Sync now", onClick = onSync)
            }
            SettingsSection("Font") {
                Choices(AppFont.entries, font, AppFont::label, onFont) {
                    MaterialTheme.typography.bodyLarge.copy(fontFamily = it.family)
                }
            }
            // The preview is above the buttons: moving to a button scrolls it into view, and the preview with it.
            SettingsSection("Subtitles") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PREVIEW_PICTURE)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    SubtitleLine(text = "This is how subtitles look.", size = subtitleSize)
                }
                Choices(SubtitleSize.entries, subtitleSize, SubtitleSize::label, onSubtitleSize)
            }
        }
    }
}

/** A row of buttons, one per option; the chosen one has a check mark. [textStyle] is each button's text look. */
@Composable
private fun <T> Choices(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    textStyle: @Composable (T) -> TextStyle = { LocalTextStyle.current },
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            GlassButton(
                text = if (option == selected) "✓ ${label(option)}" else label(option),
                onClick = { onSelect(option) },
                textStyle = textStyle(option),
            )
        }
    }
}

/** A titled glass section of Settings. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), fill = Glass.colors.fill, edge = Glass.colors.edge) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
