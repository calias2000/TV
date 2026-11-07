package com.calias.tv.app.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.calias.tv.core.epg.Program
import java.text.DateFormat
import java.util.Date

/** How many programs are listed after the one on now. */
private const val UPCOMING = 3

/**
 * What the guide says about one channel: its name, what is on now with the time and progress, and the
 * next [UPCOMING] programs. [schedule] comes from `Guide.schedule`, so it starts with the running
 * program (if any). Theme colors: it sits on the Live screen, not on the video.
 */
@Composable
fun ProgramInfo(name: String, schedule: List<Program>, nowMs: Long, modifier: Modifier = Modifier) {
    val time = remember { DateFormat.getTimeInstance(DateFormat.SHORT) }
    fun clock(ms: Long) = time.format(Date(ms))

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (schedule.isEmpty()) {
            Text(text = "No guide data", style = MaterialTheme.typography.bodySmall)
            return@Column
        }
        val running = schedule.first().takeIf { it.startMs <= nowMs }
        if (running != null) {
            Text(text = running.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = "${clock(running.startMs)} – ${clock(running.stopMs)}", style = MaterialTheme.typography.bodySmall)
            ProgressBar(running.progress(nowMs))
        }
        val upcoming = schedule.drop(if (running != null) 1 else 0).take(UPCOMING)
        if (upcoming.isNotEmpty()) {
            Text(text = "Next", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            upcoming.forEach {
                Text(
                    text = "${clock(it.startMs)}  ${it.title}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
