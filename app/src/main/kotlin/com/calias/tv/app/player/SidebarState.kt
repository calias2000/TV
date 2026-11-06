package com.calias.tv.app.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class SidebarLevel { CHANNELS, GROUPS }

/**
 * Whether the browse sidebar is open, which level it shows (the channels of a group, or the list
 * of groups) and which group is being browsed. A plain state holder: UI logic only.
 * Live: LEFT goes from channels to groups, RIGHT goes back, then closes. Movies and episodes
 * have a single group, so there is no groups level.
 */
@Stable
class SidebarState {
    var visible by mutableStateOf(false)
        private set
    var level by mutableStateOf(SidebarLevel.CHANNELS)
        private set
    var group by mutableIntStateOf(0)
        private set

    fun open(playingGroup: Int) {
        group = playingGroup
        level = SidebarLevel.CHANNELS
        visible = true
    }

    fun close() {
        visible = false
    }

    fun showGroups() {
        level = SidebarLevel.GROUPS
    }

    fun showChannels(group: Int) {
        this.group = group
        level = SidebarLevel.CHANNELS
    }
}
