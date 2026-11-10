package com.calias.tv.app.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.media3.common.PlaybackException
import com.calias.tv.app.ui.AppFont
import com.calias.tv.app.ui.AppTheme
import com.calias.tv.app.ui.SubtitleSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What is drawn on top of the video, with made-up values. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class OverlaysTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) =
        compose.setContent { AppTheme(dark = true, font = AppFont.SYSTEM, content = content) }

    @Test
    fun `the movie bar shows the title, the times and both buttons`() {
        show {
            VodOverlay(visible = true, title = "Some Film", positionMs = { 65_000 }, durationMs = { 3_600_000 }, highlighted = null)
        }
        compose.onNodeWithText("Some Film").assertIsDisplayed()
        compose.onNodeWithText("1:05").assertIsDisplayed()
        compose.onNodeWithText("1:00:00").assertIsDisplayed()
        compose.onNodeWithText("Audio").assertIsDisplayed()
        compose.onNodeWithText("Subtitles").assertIsDisplayed()
    }

    @Test
    fun `a hidden movie bar is not there`() {
        show { VodOverlay(visible = false, title = "Some Film", positionMs = { 0 }, durationMs = { 1 }, highlighted = null) }
        compose.onNodeWithText("Some Film").assertDoesNotExist()
    }

    @Test
    fun `the big icon says pause while playing`() {
        show { PlayPauseIcon(visible = true, paused = false) }
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }

    @Test
    fun `the big icon says play while paused`() {
        show { PlayPauseIcon(visible = true, paused = true) }
        compose.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test
    fun `the next episode card names the episode`() {
        show { NextEpisodeCard(visible = true, title = "S01E02 - Two", remainingFraction = { 0.5f }) }
        compose.onNodeWithText("Next episode  ·  OK to play").assertIsDisplayed()
        compose.onNodeWithText("S01E02 - Two").assertIsDisplayed()
    }

    @Test
    fun `the subtitle line is shown`() {
        show { SubtitleText(text = "Hello there", size = SubtitleSize.MEDIUM, raised = false) }
        compose.onNodeWithText("Hello there").assertIsDisplayed()
    }

    @Test
    fun `a network failure is explained in words, with the code below`() {
        val error = PlaybackException("net", null, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
        show { ErrorPanel(failure = Failure.of(error), onRetry = {}) }
        compose.onNodeWithText("Can't reach the stream").assertIsDisplayed()
        compose.onNodeWithText("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `a missing stream and any other failure have their own words`() {
        val missing = PlaybackException("404", null, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        show { ErrorPanel(failure = Failure.of(missing), onRetry = {}) }
        compose.onNodeWithText("Stream not available").assertIsDisplayed()
    }

    @Test
    fun `an unknown failure just says playback failed`() {
        val other = PlaybackException("x", null, PlaybackException.ERROR_CODE_DECODING_FAILED)
        show { ErrorPanel(failure = Failure.of(other), onRetry = {}) }
        compose.onNodeWithText("Playback failed").assertIsDisplayed()
    }

    @Test
    fun `a live stream that could not be tuned in again says it ended, with no code`() {
        show { ErrorPanel(failure = Failure.ENDED, onRetry = {}) }
        compose.onNodeWithText("The stream ended").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }
}
