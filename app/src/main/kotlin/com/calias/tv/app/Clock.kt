package com.calias.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private const val TICK_MS = 30_000L

/**
 * The current time in epoch milliseconds, refreshed on every :00 and :30 of the clock, so a minute
 * display changes on the minute and "now on air" stays right.
 */
@Composable
fun rememberNowMs(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS - System.currentTimeMillis() % TICK_MS)
            now = System.currentTimeMillis()
        }
    }
    return now
}

private val time24 = DateTimeFormatter.ofPattern("HH:mm")

/** [ms] as a 24-hour time of day in the device's time zone, for example "21:30". Never AM/PM. */
fun formatTime(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(time24)
