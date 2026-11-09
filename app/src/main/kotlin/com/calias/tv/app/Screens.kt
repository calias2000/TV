package com.calias.tv.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassButton
import com.calias.tv.app.ui.GlassPanel
import com.calias.tv.app.ui.LocalBackdrop
import com.calias.tv.app.ui.Spinner

/** What the playlist state says, for Settings and for Home's status pill. Null while loading: a spinner shows then. */
internal fun statusText(state: PlaylistState): String? = when (state) {
    PlaylistState.Loading -> null
    is PlaylistState.Loaded -> "Playlist loaded: ${state.playlist.channels.size} entries"
    is PlaylistState.Failed -> "Playlist unavailable: ${state.message}. Press Sync to retry."
}

/** Settings: two glass sections, the playlist (source, status, sync) and the look (font). */
@Composable
fun SettingsScreen(state: PlaylistState, onSync: () -> Unit, font: AppFont, onFont: (AppFont) -> Unit) {
    // No glow here: the screens before it leave theirs on.
    val backdrop = LocalBackdrop.current
    LaunchedEffect(Unit) { backdrop.color = null }
    Column(
        modifier = Modifier.fillMaxSize().padding(Edge.Margin),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        SettingsSection("Playlist") {
            Text(text = PlaylistLoader.PLAYLIST_URL, color = Glass.colors.textMuted)
            statusText(state)?.let { Text(text = it) } ?: Spinner(diameter = 24.dp)
            GlassButton(text = "Sync now", onClick = onSync)
        }
        SettingsSection("Font") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppFont.entries.forEach { option ->
                    GlassButton(
                        text = if (option == font) "✓ ${option.label}" else option.label,
                        onClick = { onFont(option) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = option.family),
                    )
                }
            }
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
