package com.calias.tv.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
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
import com.calias.tv.app.ui.Space
import com.calias.tv.app.ui.Spinner
import com.calias.tv.app.update.UpdateState
import com.calias.tv.app.update.updateButton
import com.calias.tv.app.update.updateText
import com.calias.tv.core.profile.Profile
import com.calias.tv.app.ui.fadeEdges

/**
 * Settings: glass sections for the profile (status, sync, edit), the app version (update) and the font. The
 * profile and the version sit side by side, as tall as each other; the font has a row of buttons too wide
 * for half the screen, so it has its own line. The sections scroll when they do not fit the screen.
 */
@Composable
fun SettingsScreen(
    profile: Profile,
    state: PlaylistState,
    onSync: () -> Unit,
    onEditProfile: () -> Unit,
    font: AppFont,
    onFont: (AppFont) -> Unit,
    update: UpdateState = UpdateState.Idle,
    onUpdate: () -> Unit = {},
) {
    // No glow here: the screens before it leave theirs on.
    val backdrop = LocalBackdrop.current
    LaunchedEffect(Unit) { backdrop.color = null }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = Edge.Margin).padding(top = Edge.Margin)) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        // Padding inside the scroll, so the focus rings at the ends are not faded.
        Column(
            modifier = Modifier.fillMaxSize().fadeEdges().verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(SECTION_GAP),
        ) {
            Row(modifier = Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(SECTION_GAP)) {
                SettingsSection("Profile: ${profile.name}", Modifier.weight(1f).fillMaxHeight()) {
                    statusText(state)?.let { Text(text = it) } ?: Spinner(diameter = 24.dp)
                    Row(horizontalArrangement = Arrangement.spacedBy(Space.Gap)) {
                        GlassButton(text = "Sync now", onClick = onSync)
                        GlassButton(text = "Edit profile", onClick = onEditProfile)
                    }
                }
                SettingsSection("App version ${BuildConfig.VERSION_NAME}", Modifier.weight(1f).fillMaxHeight()) {
                    updateText(update)?.let { Text(text = it) }
                    updateButton(update)?.let { GlassButton(text = it, onClick = onUpdate) }
                }
            }
            SettingsSection("Font", Modifier.fillMaxWidth()) {
                Choices(AppFont.entries, font, AppFont::label, onFont) {
                    MaterialTheme.typography.bodyLarge.copy(fontFamily = it.family)
                }
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
    Row(horizontalArrangement = Arrangement.spacedBy(Space.Gap)) {
        options.forEach { option ->
            GlassButton(
                text = if (option == selected) "✓ ${label(option)}" else label(option),
                onClick = { onSelect(option) },
                textStyle = textStyle(option),
            )
        }
    }
}

private val SECTION_GAP = 24.dp

/** A titled glass section of Settings. [modifier] says how much room it takes. */
@Composable
private fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    GlassPanel(modifier = modifier, fill = Glass.colors.fill, edge = Glass.colors.edge) {
        Column(modifier = Modifier.padding(Space.Panel), verticalArrangement = Arrangement.spacedBy(Space.Gap)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
