package com.calias.tv.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.calias.tv.app.browse.BrowseList
import com.calias.tv.app.browse.ChannelRow
import com.calias.tv.app.ui.GlassButton
import com.calias.tv.app.ui.GlassDialog
import com.calias.tv.app.ui.GlassSideSheet
import com.calias.tv.app.ui.GlassTextField
import com.calias.tv.app.ui.Space
import com.calias.tv.app.ui.Spinner
import com.calias.tv.core.profile.Profile
import com.calias.tv.core.profile.Profiles
import com.calias.tv.core.profile.Source
import kotlinx.coroutines.launch

/** The profile modal that is open: the list to switch or add, or the form to add (`id` null) or edit the profile called `id`. */
sealed interface ProfileDialog {
    data object Switch : ProfileDialog
    data class Form(val id: String?) : ProfileDialog
}

/**
 * Shows the profile modal [dialog] over whatever screen it is called from, and carries out what it asks
 * of [profiles]. [onChange] sets the modal to show next (null closes it): a screen only needs to say which
 * modal it wants, and this is the one place that knows what they do.
 */
@Composable
fun ProfileDialogHost(dialog: ProfileDialog?, state: ProfilesState, profiles: ProfilesViewModel, onChange: (ProfileDialog?) -> Unit) {
    val close = { onChange(null) }
    when (dialog) {
        null -> Unit
        ProfileDialog.Switch -> GlassSideSheet(
            title = "Profiles",
            onDismiss = close,
            footer = if (Profiles.isFull(state.profiles)) null else ({
                GlassButton(text = "Add profile", onClick = { onChange(ProfileDialog.Form(null)) }, modifier = Modifier.fillMaxWidth())
            }),
        ) {
            ProfileList(
                state = state,
                onPick = {
                    profiles.switchTo(it.id)
                    close()
                },
            )
        }
        is ProfileDialog.Form -> {
            val existing = state.profiles.firstOrNull { it.id == dialog.id }
            GlassDialog(
                title = if (existing == null) "New profile" else "Edit profile",
                onDismiss = close,
                action = existing?.let {
                    {
                        DeleteProfileButton(onDelete = {
                            profiles.delete(it.id)
                            close()
                        })
                    }
                },
            ) {
                ProfileForm(
                    existing = existing,
                    defaultName = Profiles.defaultName(state.profiles),
                    serverBase = BuildConfig.BACKEND_BASE,
                    onSave = { name, source -> profiles.save(existing, name, source) },
                    onDone = close,
                )
            }
        }
    }
}

/** The profiles of this TV as the app's usual list rows (like the episodes of a show), the active one checked. Focus starts on the active row. */
@Composable
internal fun ProfileList(state: ProfilesState, onPick: (Profile) -> Unit) {
    BrowseList(
        count = state.profiles.size,
        focusIndex = state.profiles.indexOfFirst { it.id == state.activeId }.coerceAtLeast(0),
        contentPadding = PaddingValues(horizontal = Space.FocusEdge, vertical = Space.Gap),
    ) { index, focus ->
        val profile = state.profiles[index]
        ChannelRow(title = profile.name, trailing = if (profile.id == state.activeId) "✓" else null, modifier = focus, onClick = { onPick(profile) })
    }
}

/**
 * The fields of a profile: a name and the playlist, typed as an access code or as a full address (see
 * [Source.parse]; [serverBase] is the server codes belong to). [existing] is the profile being edited, or null
 * for a new one. [onSave] checks that the playlist loads and saves, returning why it failed or null; then
 * [onDone] runs. Focus starts on the field most likely to need typing: the address of a new profile, the
 * name of an existing one.
 */
@Composable
internal fun ProfileForm(
    existing: Profile?,
    defaultName: String,
    serverBase: String,
    onSave: suspend (name: String, source: Source) -> String?,
    onDone: () -> Unit,
) {
    val name = rememberTextFieldState(existing?.name ?: defaultName)
    val address = rememberTextFieldState((existing?.source as? Source.Direct)?.url.orEmpty())
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val startFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { startFocus.requestFocus() }

    fun save() {
        val cleanName = Profiles.cleanName(name.text.toString())
        val typed = address.text.toString()
        val source = Source.parse(typed, serverBase)
        problem = when {
            busy -> return
            cleanName.isEmpty() -> "Give the profile a name."
            source == null && Source.code(typed) != null -> "This app was built without a server address, so codes do not work. Type the full address."
            source == null -> "Enter the code you were given, or an address that starts with http:// or https:// and has no spaces."
            else -> null
        }
        if (problem != null || source == null) return
        busy = true
        scope.launch {
            val failure = onSave(cleanName, source)
            busy = false
            if (failure == null) onDone() else problem = failure
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.Gap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.Gap)) {
            GlassTextField(
                state = name,
                label = "Name",
                modifier = Modifier.weight(1f),
                focusRequester = startFocus.takeIf { existing != null },
                onDone = { focus.moveFocus(FocusDirection.Next) },
            )
            GlassTextField(
                state = address,
                label = "Playlist address or code",
                modifier = Modifier.weight(2f),
                focusRequester = startFocus.takeIf { existing == null },
                keyboardType = KeyboardType.Uri,
                onDone = ::save,
            )
        }
        when {
            busy -> Row(horizontalArrangement = Arrangement.spacedBy(Space.Gap), verticalAlignment = Alignment.CenterVertically) {
                Spinner(diameter = 24.dp)
                Text(text = "Checking the playlist…")
            }
            problem != null -> Text(text = problem.orEmpty())
        }
        GlassButton(text = "Save", onClick = ::save, modifier = Modifier.fillMaxWidth())
    }
}

/** The button that deletes a profile. It asks to be pressed twice. */
@Composable
internal fun DeleteProfileButton(onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    GlassButton(
        text = if (confirm) "Press again to delete" else "Delete profile",
        onClick = { if (confirm) onDelete() else confirm = true },
    )
}
