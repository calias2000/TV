package com.calias.tv.app.update

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** GitHub's "latest release" answer (the shape is the real one, cut down to what matters), read into a [Release]. */
@RunWith(RobolectricTestRunner::class)
class ReleaseTest {

    private fun answer(assets: String, tag: String = "v0.2.0") = """
        {"tag_name": "$tag", "draft": false, "prerelease": false, "assets": [$assets]}
    """.trimIndent()

    private val apk = """
        {"name": "app-release.apk", "size": 2543910,
         "digest": "sha256:1dd01c34a2ca4e7b7890cc9c81fdaea3d4f5ab6f9b6b932d46e1b32201a1b50c",
         "browser_download_url": "https://github.com/o/TV/releases/download/v0.2.0/app-release.apk"}
    """.trimIndent()

    @Test
    fun `the release is read from its tag and its APK`() {
        assertEquals(
            Release(
                version = "0.2.0",
                apkUrl = "https://github.com/o/TV/releases/download/v0.2.0/app-release.apk",
                sizeBytes = 2543910,
                sha256 = "1dd01c34a2ca4e7b7890cc9c81fdaea3d4f5ab6f9b6b932d46e1b32201a1b50c",
            ),
            Releases.parse(answer(apk)),
        )
    }

    @Test
    fun `other files in the release are ignored, and no APK means no release`() {
        val notes = """{"name": "notes.txt", "size": 5, "digest": null, "browser_download_url": "https://x/notes.txt"}"""
        assertEquals("0.2.0", Releases.parse(answer("$notes, $apk"))?.version)
        assertNull(Releases.parse(answer(notes)))
        assertNull(Releases.parse(answer("")))
    }

    @Test
    fun `an APK that would be downloaded over plain http is not a release`() {
        assertNull(Releases.parse(answer(apk.replace("https://", "http://"))))
    }

    @Test
    fun `a missing or odd digest is no checksum, not an error`() {
        fun checksum(digest: String) = Releases.parse(answer(apk.replace(Regex("\"digest\": \"[^\"]*\""), "\"digest\": $digest")))?.sha256
        assertNull(checksum("null"))
        assertNull(checksum("\"sha1:abc\""))
        assertEquals("1".repeat(64), checksum("\"sha256:${"1".repeat(64)}\""))
    }

    @Test
    fun `an answer that is not a release is refused`() {
        listOf("", "not json", "{}", "[]", """{"message": "Not Found"}""").forEach { assertNull(Releases.parse(it), "'$it'") }
    }
}
