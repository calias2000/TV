package com.calias.tv.app.update

import org.json.JSONObject

/** A release on GitHub: its version, the APK to download, and that APK's size and SHA-256 (null when GitHub gives none). */
data class Release(val version: String, val apkUrl: String, val sizeBytes: Long, val sha256: String?)

object Releases {

    /** GitHub's "latest release" of this app: the newest one that is not a draft or a pre-release. */
    const val LATEST_URL = "https://api.github.com/repos/calias2000/TV/releases/latest"

    // The file the release workflow publishes: Gradle's own name for the release build.
    private const val APK_NAME = "app-release.apk"

    /**
     * The release in GitHub's answer for [LATEST_URL], or null if it is not one or has no APK. The APK must come
     * from an https address: the app allows plain http for streams, which must never reach an update.
     */
    fun parse(json: String): Release? = try {
        val release = JSONObject(json)
        val assets = release.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name") == APK_NAME && it.getString("browser_download_url").startsWith("https://") }
        apk?.let {
            Release(
                version = release.getString("tag_name").removePrefix("v"),
                apkUrl = it.getString("browser_download_url"),
                sizeBytes = it.getLong("size"),
                sha256 = it.optString("digest").removePrefix("sha256:").takeIf { digest -> digest.length == 64 },
            )
        }
    } catch (e: org.json.JSONException) {
        null
    }
}
