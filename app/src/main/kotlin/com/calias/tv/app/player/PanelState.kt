package com.calias.tv.app.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class PanelLevel { CHANNELS, GROUPS }

/**
 * Whether the browse panel is open, which level it shows (the channels of a group, or the list of
 * groups) and which group is being browsed. A plain state holder: UI logic only.
 * Live opens with the panel already open ([startOpen]); movies and episodes start with it closed and
 * have a single group, so there is no groups level for them.
 */
@Stable
class PanelState(startOpen: Boolean, group: Int) {
    var visible by mutableStateOf(startOpen)
        private set
    var level by mutableStateOf(PanelLevel.CHANNELS)
        private set
    var group by mutableIntStateOf(group)
        private set

    fun open(playingGroup: Int) {
        group = playingGroup
        level = PanelLevel.CHANNELS
        visible = true
    }

    fun close() {
        visible = false
    }

    fun showGroups() {
        level = PanelLevel.GROUPS
    }

    fun showChannels(group: Int) {
        this.group = group
        level = PanelLevel.CHANNELS
    }
}
