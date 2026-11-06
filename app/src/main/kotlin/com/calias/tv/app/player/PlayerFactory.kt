package com.calias.tv.app.player

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.util.EventLogger

/**
 * The single place where the ExoPlayer is configured. The screen only asks for a player.
 *
 * Uses Media3 classes marked @UnstableApi. Media3 is pinned (see libs.versions.toml), so they cannot
 * change without a version bump. The opt-in is limited to the player files on purpose.
 * Options not enabled yet, and why: Notes/MyTV/ideation/future-player-features.md.
 */
@OptIn(UnstableApi::class)
fun buildPlayer(context: Context): ExoPlayer {
    // Providers may redirect https -> http. Media3 refuses that by default, so allow it.
    val httpFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
    val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory)),
        )
        .build()

    // Debug builds only: Media3's own EventLogger prints the chosen decoder, video size changes,
    // dropped frames and unsupported tracks to Logcat (tag "EventLogger"). This is the tool the
    // Media3 troubleshooting page recommends when content fails to play without an error.
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (isDebuggable) player.addAnalyticsListener(EventLogger())
    return player
}
