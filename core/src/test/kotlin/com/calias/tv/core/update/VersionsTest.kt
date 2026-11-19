package com.calias.tv.core.update

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionsTest {

    @Test
    fun `a higher number in any place is newer, the first difference decides`() {
        assertTrue(Versions.isNewer("0.1.1", "0.1.0"))
        assertTrue(Versions.isNewer("0.2.0", "0.1.9"))
        assertTrue(Versions.isNewer("1.0.0", "0.9.9"))
        assertFalse(Versions.isNewer("0.1.9", "0.2.0"))
    }

    @Test
    fun `numbers are compared as numbers, not as text`() {
        assertTrue(Versions.isNewer("0.10.0", "0.9.0"))
        assertFalse(Versions.isNewer("0.9.0", "0.10.0"))
    }

    @Test
    fun `the same version is not newer, with or without the v and the trailing zeros`() {
        assertFalse(Versions.isNewer("v0.1.0", "0.1.0"))
        assertFalse(Versions.isNewer("0.1", "0.1.0"))
        assertTrue(Versions.isNewer("0.1.0.1", "0.1.0"))
    }

    @Test
    fun `a tag that is not plain numbers is never newer`() {
        listOf("", "v", "latest", "v1.0-beta", "1.0.x", "1..0", "-1.0.0").forEach {
            assertFalse(Versions.isNewer(it, "0.1.0"), "'$it'")
        }
        assertFalse(Versions.isNewer("v0.2.0", "dev"), "an installed version that is not numbers cannot be compared")
    }
}
