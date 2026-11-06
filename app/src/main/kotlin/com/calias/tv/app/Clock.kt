package com.calias.tv.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

private val TICK = 30.seconds

/** The current time in epoch milliseconds, refreshed every 30 s so "now on air" stays right. */
@Composable
fun rememberNowMs(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK)
            now = System.currentTimeMillis()
        }
    }
    return now
}
