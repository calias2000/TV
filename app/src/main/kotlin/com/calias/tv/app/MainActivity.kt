package com.calias.tv.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme

/** The single activity. Screens and navigation live in AppNav. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: PlaylistViewModel = viewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                AppNav(state = state, onSync = viewModel::sync)
            }
        }
    }
}
