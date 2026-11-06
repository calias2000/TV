package com.calias.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

/** The single activity. Screens and navigation live in AppNav. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val prefs: PrefsViewModel = viewModel()
            // Dark or light, switched from Home and remembered between runs.
            val dark by prefs.dark.collectAsStateWithLifecycle()
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                // One root surface gives every screen the same background and readable text color.
                Surface(modifier = Modifier.fillMaxSize(), shape = RectangleShape) {
                    val viewModel: PlaylistViewModel = viewModel()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    val guide by viewModel.guide.collectAsStateWithLifecycle()
                    AppNav(
                        state = state,
                        guide = guide,
                        onSync = viewModel::sync,
                        dark = dark,
                        onToggleTheme = prefs::toggleDark,
                        prefs = prefs,
                    )
                }
            }
        }
    }
}
