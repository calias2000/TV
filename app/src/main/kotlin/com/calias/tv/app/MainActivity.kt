package com.calias.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.calias.tv.app.ui.AppBackground
import com.calias.tv.app.ui.AppTheme

/** The single activity. Screens and navigation live in AppNav. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs: PrefsViewModel = viewModel()
            // Dark or light, switched from Home and remembered between runs.
            val dark by prefs.dark.collectAsStateWithLifecycle()
            val font by prefs.font.collectAsStateWithLifecycle()
            AppTheme(dark = dark, font = font) {
                // One shared background behind every screen.
                AppBackground {
                    val viewModel: PlaylistViewModel = viewModel()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val guide by viewModel.guide.collectAsStateWithLifecycle()
                    AppNav(
                        state = state,
                        guide = guide,
                        onSync = viewModel::sync,
                        onToggleTheme = prefs::toggleDark,
                        font = font,
                        onFont = prefs::setFont,
                        prefs = prefs,
                    )
                }
            }
        }
    }
}
