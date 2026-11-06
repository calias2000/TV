package com.calias.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
            // Dark or light, switched from Home. Survives screen changes; resets on a full app restart.
            var dark by rememberSaveable { mutableStateOf(true) }
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                // One root surface gives every screen the same background and readable text color.
                Surface(modifier = Modifier.fillMaxSize(), shape = RectangleShape) {
                    val viewModel: PlaylistViewModel = viewModel()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    AppNav(
                        state = state,
                        onSync = viewModel::sync,
                        dark = dark,
                        onToggleTheme = { dark = !dark },
                    )
                }
            }
        }
    }
}
