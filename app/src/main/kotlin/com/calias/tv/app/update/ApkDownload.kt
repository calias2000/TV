package com.calias.tv.app.update

import com.calias.tv.app.httpGet
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.time.Duration.Companion.minutes

/**
 * Downloads the APK of [release] into [target] (replacing what is there) and reports the percentage done. A file whose
 * SHA-256 is not the one GitHub gave for the release is deleted and refused, so a damaged download never reaches the installer.
 */
internal suspend fun downloadApk(release: Release, target: File, onProgress: (percent: Int) -> Unit) {
    target.parentFile?.mkdirs()
    try {
        val digest = MessageDigest.getInstance("SHA-256")
        httpGet(release.apkUrl, "Update", 10.minutes) { stream ->
            target.outputStream().use { out ->
                val buffer = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    done += read
                    onProgress(if (release.sizeBytes > 0) (done * 100 / release.sizeBytes).toInt().coerceIn(0, 100) else 0)
                }
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (release.sha256 != null && !actual.equals(release.sha256, ignoreCase = true)) throw IOException("The download is damaged: its checksum is not the release's")
    } catch (e: Exception) {
        target.delete()
        throw e
    }
}
