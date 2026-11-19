package com.calias.tv.core.update

/** App versions as GitHub tags and `versionName` write them: numbers separated by dots, like "0.2.1" or "v0.2.1". */
object Versions {

    /**
     * Whether [remote] is a newer version than [installed]. The numbers are compared one by one, so 0.10.0 is
     * newer than 0.9.0. Anything that is not numbers separated by dots (a tag like "v1.0-beta") is never
     * newer, so a pre-release cannot be offered by mistake.
     */
    fun isNewer(remote: String, installed: String): Boolean {
        val newer = numbers(remote) ?: return false
        val current = numbers(installed) ?: return false
        val length = maxOf(newer.size, current.size)
        for (i in 0 until length) {
            val difference = newer.getOrElse(i) { 0 } - current.getOrElse(i) { 0 }
            if (difference != 0) return difference > 0
        }
        return false
    }

    private fun numbers(version: String): List<Int>? =
        version.removePrefix("v").split('.').map { it.toIntOrNull()?.takeIf { n -> n >= 0 } ?: return null }
}
