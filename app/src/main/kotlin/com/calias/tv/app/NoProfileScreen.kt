package com.calias.tv.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.ui.Edge
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.GlassSurface
import com.calias.tv.app.ui.LocalBackdrop
import com.calias.tv.app.ui.Space

/**
 * What shows while no profile is active, in the look of Home: the clock and one tile. With no profile at
 * all (a first start) the tile adds one; if profiles exist but none is chosen (the active one was
 * deleted) it opens the list. Either way [onOpen] asks for the right profile modal. The tile is the only
 * thing to press, so its edge is lit at rest: it looks selectable even before the remote moves.
 */
@Composable
fun NoProfileScreen(hasProfiles: Boolean, onOpen: () -> Unit) {
    // No glow here: the screen before it may have left one on.
    val backdrop = LocalBackdrop.current
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        backdrop.color = null
        focus.requestFocus()
    }
    Column(modifier = Modifier.fillMaxSize().padding(Edge.Margin)) {
        Clock(rememberNowMs())
        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            GlassSurface(
                onClick = onOpen,
                modifier = Modifier.width(420.dp).height(150.dp).focusRequester(focus),
                colors = Glass.colors.copy(edge = Glass.colors.edgeFocused),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = Space.Inner),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = if (hasProfiles) "Choose profile" else "Add profile", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    if (!hasProfiles) {
                        Text(text = "Add a playlist to start watching", color = Glass.colors.textMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
