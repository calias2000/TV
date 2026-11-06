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
 * change without a version bump. The opt-in is limited to this function on purpose.
 *
 * Options that exist but are NOT enabled yet, because no stream has needed them (Media3 docs,
 * troubleshooting page): MPEG-TS extractor flags FLAG_DETECT_ACCESS_UNITS and
 * FLAG_ALLOW_NON_IDR_KEYFRAMES (for TS streams that look stuck buffering; extra CPU, possible brief
 * visual corruption at start), decoder fallback (DefaultRenderersFactory.setEnableDecoderFallback),
 * custom buffering (LoadControl), custom retry policy (LoadErrorHandlingPolicy).
 * Add one only when a real stream shows the problem it solves.
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
