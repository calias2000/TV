package com.calias.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calias.tv.app.ui.AppBackground
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.app.update.UpdateViewModel
import com.calias.tv.core.profile.Profile

/** The single activity. It shows the app for the active profile, or the way to get one; screens and navigation live in AppNav. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs: PrefsViewModel = viewModel()
            val profiles: ProfilesViewModel = viewModel()
            val update: UpdateViewModel = viewModel()
            // The look of the active profile, and the profiles themselves, are remembered between runs. Nothing is drawn until they are read.
            val look = prefs.look.collectAsStateWithLifecycle().value ?: return@setContent
            val profilesState = profiles.state.collectAsStateWithLifecycle().value ?: return@setContent
            val active = profilesState.active
            var dialog by remember { mutableStateOf<ProfileDialog?>(null) }
            AppTheme(dark = look.dark, font = look.font) {
                AppBackground {
                    if (active != null) {
                        ProfileContent(active, look, prefs, update, onProfileDialog = { dialog = it })
                    } else {
                        val hasProfiles = profilesState.profiles.isNotEmpty()
                        NoProfileScreen(hasProfiles, onOpen = { dialog = if (hasProfiles) ProfileDialog.Switch else ProfileDialog.Form(null) })
                    }
                    ProfileDialogHost(dialog, profilesState, profiles, onChange = { dialog = it })
                }
            }
        }
    }
}

/** The app for one active profile. A different profile starts it over, on Home. */
@Composable
private fun ProfileContent(profile: Profile, look: Look, prefs: PrefsViewModel, update: UpdateViewModel, onProfileDialog: (ProfileDialog) -> Unit) {
    val viewModel: PlaylistViewModel = viewModel()
    LaunchedEffect(profile.source) { viewModel.use(profile.source) }
    // While the app is in front, keep the guide up to date (it also runs again on every return).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(viewModel) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.keepGuideFresh() } }
    val source by viewModel.source.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val guide by viewModel.guide.collectAsStateWithLifecycle()
    // Wait until the playlist model is on this profile's source, so one profile never shows another's channels.
    if (source == profile.source) {
        key(profile.id) {
            AppNav(
                profile = profile,
                onProfileDialog = onProfileDialog,
                state = state,
                guide = guide,
                onSync = viewModel::sync,
                look = look,
                prefs = prefs,
                update = update,
            )
        }
    }
}
