package com.calias.tv.app.player

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
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
 */
@OptIn(UnstableApi::class)
fun buildPlayer(context: Context): ExoPlayer {
    // Providers may redirect https -> http. Media3 refuses that by default, so allow it.
    val httpFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
    val player = ExoPlayer.Builder(context)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(DefaultDataSource.Factory(context, httpFactory)),
        )
        // Audio focus: other apps and the assistant can lower or pause us, and we pause for them. Without
        // this ExoPlayer plays over everything. The player also pauses when the audio output goes away.
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    // Debug builds only: Media3's EventLogger prints decoders, tracks and dropped frames to Logcat
    // (tag "EventLogger"), the tool its troubleshooting page recommends when playback fails silently.
    val isDebuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    if (isDebuggable) player.addAnalyticsListener(EventLogger())
    return player
}
