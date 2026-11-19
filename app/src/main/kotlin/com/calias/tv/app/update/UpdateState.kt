package com.calias.tv.app.update

/** Where the app's own update is: nothing asked yet, checking, none or one found, being downloaded and installed, or failed. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(override val release: Release) : UpdateState
    data class Downloading(override val release: Release, val percent: Int) : UpdateState
    data class Installing(override val release: Release) : UpdateState

    /** [release] is set when the failure is of an update that was found, so it can be tried again. */
    data class Failed(val message: String, override val release: Release? = null) : UpdateState

    /** The update this state is about, if there is one. */
    val release: Release? get() = null
}

/** What Settings says about the update. */
internal fun updateText(state: UpdateState): String? = when (state) {
    UpdateState.Idle -> null
    UpdateState.Checking -> "Checking for updates…"
    UpdateState.UpToDate -> "You have the latest version."
    is UpdateState.Available -> "Version ${state.release.version} is available."
    is UpdateState.Downloading -> "Downloading version ${state.release.version}: ${state.percent}%"
    is UpdateState.Installing -> "Installing version ${state.release.version}. The app closes when it is done: open it again."
    is UpdateState.Failed -> "Update failed: ${state.message}"
}

/** The label of the button that acts on the update in Settings; null while something is running. */
internal fun updateButton(state: UpdateState): String? = when (state) {
    is UpdateState.Available -> "Install version ${state.release.version}"
    is UpdateState.Failed -> if (state.release != null) "Try again" else "Check for updates"
    UpdateState.Idle, UpdateState.UpToDate -> "Check for updates"
    UpdateState.Checking, is UpdateState.Downloading, is UpdateState.Installing -> null
}

/** The label of the button on Home, which shows only when an update was found (null: no button). */
internal fun homeUpdateButton(state: UpdateState): String? = when (state) {
    is UpdateState.Available -> "Update to ${state.release.version}"
    is UpdateState.Downloading -> "Downloading ${state.percent}%"
    is UpdateState.Installing -> "Installing…"
    is UpdateState.Failed -> if (state.release != null) "Retry update" else null
    else -> null
}
