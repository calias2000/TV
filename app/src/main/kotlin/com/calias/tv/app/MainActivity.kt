package com.calias.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calias.tv.app.ui.AppBackground
import com.calias.tv.app.ui.AppTheme

/** The single activity. Screens and navigation live in AppNav. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs: PrefsViewModel = viewModel()
            // Dark or light and the font, remembered between runs. Nothing is drawn until they are read.
            val look = prefs.look.collectAsStateWithLifecycle().value ?: return@setContent
            AppTheme(dark = look.dark, font = look.font) {
                // One shared background behind every screen.
                AppBackground {
                    val viewModel: PlaylistViewModel = viewModel()
                    // While the app is in front, keep the guide up to date (it also runs again on every return).
                    val lifecycle = LocalLifecycleOwner.current.lifecycle
                    LaunchedEffect(viewModel) { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.keepGuideFresh() } }
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val guide by viewModel.guide.collectAsStateWithLifecycle()
                    AppNav(
                        state = state,
                        guide = guide,
                        onSync = viewModel::sync,
                        onToggleTheme = prefs::toggleDark,
                        look = look,
                        prefs = prefs,
                    )
                }
            }
        }
    }
}
