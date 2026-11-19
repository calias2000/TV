package com.calias.tv.app.update

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.calias.tv.app.BuildConfig
import com.calias.tv.app.httpGet
import com.calias.tv.core.update.Versions
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The app's own updates, straight from GitHub Releases: it checks once when the app starts (and when asked),
 * and installs the new version when told to. Nothing is installed without the user pressing a button.
 * Installing needs the "install unknown apps" permission for this app; if it is missing the system's
 * screen for it opens, and the next press installs.
 */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val installStatus = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onInstallStatus(intent)
    }

    init {
        ContextCompat.registerReceiver(app, installStatus, IntentFilter(ApkInstaller.STATUS_ACTION), ContextCompat.RECEIVER_NOT_EXPORTED)
        check()
    }

    /** What the button for the update does: install one that was found, else look for one. */
    fun act() {
        if (_state.value.release != null) install() else check()
    }

    private fun busy() = _state.value.let { it is UpdateState.Checking || it is UpdateState.Downloading || it is UpdateState.Installing }

    private fun check() {
        if (busy()) return
        viewModelScope.launch {
            _state.value = UpdateState.Checking
            _state.value = try {
                val headers = mapOf("Accept" to "application/vnd.github+json", "User-Agent" to "calias-tv")
                val release = Releases.parse(httpGet(Releases.LATEST_URL, "Update", 20.seconds, headers) { it.bufferedReader().readText() })
                when {
                    release == null -> UpdateState.Failed("The latest release has no app file")
                    Versions.isNewer(release.version, BuildConfig.VERSION_NAME) -> UpdateState.Available(release)
                    else -> UpdateState.UpToDate
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UpdateState.Failed(e.message ?: e.toString())
            }
        }
    }

    private fun install() {
        val release = _state.value.release ?: return
        if (busy()) return
        val context = getApplication<Application>()
        if (!context.packageManager.canRequestPackageInstalls()) {
            val settings = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            context.startActivity(settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            _state.value = UpdateState.Failed("Allow installs from this app in the screen that opened, then try again.", release)
            return
        }
        viewModelScope.launch {
            // The installer copies the file into its own session, so it is not needed once that is handed over.
            val apk = File(context.filesDir, "updates/app-release.apk")
            try {
                _state.value = UpdateState.Downloading(release, 0)
                downloadApk(release, apk) { _state.value = UpdateState.Downloading(release, it) }
                _state.value = UpdateState.Installing(release)
                ApkInstaller.install(context, apk)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UpdateState.Failed(e.message ?: e.toString(), release)
            } finally {
                apk.delete()
            }
        }
    }

    private fun onInstallStatus(intent: Intent) {
        val release = _state.value.release
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            // The system wants the user's OK: show its own prompt.
            PackageInstaller.STATUS_PENDING_USER_ACTION -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                ?.let { getApplication<Application>().startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            // Installed: the system closes this app and the new version is there on the next start.
            PackageInstaller.STATUS_SUCCESS -> Unit
            // The user said no at the prompt: back to where it was.
            PackageInstaller.STATUS_FAILURE_ABORTED -> _state.value = release?.let { UpdateState.Available(it) } ?: UpdateState.Idle
            else -> _state.value = UpdateState.Failed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "The system refused the update", release)
        }
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(installStatus)
    }
}
