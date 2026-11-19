package com.calias.tv.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import java.io.File

/**
 * Installs an APK through a [PackageInstaller] session: the file is read from the app's own storage, and the
 * result comes back as a broadcast with the real reason when the system refuses it ("App not installed" alone
 * says nothing). On Android 12+ the session asks for no confirmation when the system allows it for an app
 * updating itself; otherwise the result says the user must confirm, and the system's own prompt is shown.
 */
internal object ApkInstaller {

    const val STATUS_ACTION = "com.calias.tv.app.UPDATE_STATUS"

    fun install(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("app.apk", 0, apk.length()).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                // The result is sent to our own receiver. It must be mutable: the system fills in the status.
                val result = Intent(STATUS_ACTION).setPackage(context.packageName)
                val pending = PendingIntent.getBroadcast(context, 0, result, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                session.commit(pending.intentSender)
            }
        } catch (e: Exception) {
            installer.abandonSession(sessionId) // a half-written session would keep its data until the system cleans it up
            throw e
        }
    }
}
