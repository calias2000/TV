package com.calias.tv.app.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.app.formatTime
import com.calias.tv.app.ui.Corner
import com.calias.tv.app.ui.Glass
import com.calias.tv.app.ui.ProgressBar
import com.calias.tv.app.ui.GlassSurface
import com.calias.tv.app.ui.Space
import com.calias.tv.app.ui.fadeEdges
import com.calias.tv.core.epg.Program

/**
 * What the guide says about one channel: its name, then the rest of its day as a scrolling list. The
 * running program comes first, with its progress. [schedule] comes from `Guide.schedule`. Focus can enter
 * the list to scroll through the day: [entry] is always on the row that had focus last, so it also works
 * after scrolling. Use a new instance per channel (`key`), so each channel starts at the top. Light on
 * dark, it sits on a panel over the video.
 */
@Composable
fun ProgramGuide(name: String, schedule: List<Program>, nowMs: Long, entry: FocusRequester) {
    val colors = Glass.overVideo
    var lastFocused by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.padding(horizontal = Space.Inset).padding(top = Space.Gap)) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = Space.Inset, end = Space.Inset, bottom = 4.dp),
        )
        if (schedule.isEmpty()) {
            Text(
                text = "No guide data",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = Space.Inset),
            )
            return@Column
        }
        val entryRow = lastFocused.coerceAtMost(schedule.lastIndex)
        LazyColumn(
            modifier = Modifier.fadeEdges(),
            contentPadding = PaddingValues(vertical = Space.Gap),
            verticalArrangement = Arrangement.spacedBy(Space.Row),
        ) {
            itemsIndexed(schedule) { index, program ->
                val running = program.startMs <= nowMs
                GlassSurface(
                    onClick = {}, // only focusable: it lets the remote scroll through the day
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.hasFocus) lastFocused = index }
                        .then(if (index == entryRow) Modifier.focusRequester(entry) else Modifier),
                    shape = Corner.Medium,
                    colors = colors,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = "${formatTime(program.startMs)} – ${formatTime(program.stopMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (running) colors.accent else colors.textMuted,
                        )
                        Text(text = program.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (running) ProgressBar(program.progress(nowMs), Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}
