package com.calias.tv.core.profile

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfilesTest {

    private fun profile(id: String, name: String = "Profile $id") = Profile(id, name, Source.Direct("https://a.example/$id.m3u"))

    @Test
    fun `add appends, replace keeps the position, remove drops by id`() {
        val list = Profiles.add(Profiles.add(emptyList(), profile("1")), profile("2"))
        assertEquals(listOf("1", "2"), list.map { it.id })
        assertEquals(listOf("Mum", "Profile 2"), Profiles.replace(list, profile("1", "Mum")).map { it.name })
        assertEquals(listOf("2"), Profiles.remove(list, "1").map { it.id })
        assertEquals(list, Profiles.remove(list, "gone"))
    }

    @Test
    fun `the list holds at most MAX profiles`() {
        val full = (1..Profiles.MAX).map { profile("$it") }
        assertTrue(Profiles.isFull(full))
        assertFalse(Profiles.isFull(full.drop(1)))
        assertFailsWith<IllegalArgumentException> { Profiles.add(full, profile("x")) }
    }

    @Test
    fun `a typed name is trimmed, flattened to one line and cut`() {
        assertEquals("Mum and Dad", Profiles.cleanName("  Mum \t and\nDad  "))
        assertEquals("", Profiles.cleanName(" \t "))
        assertEquals(30, Profiles.cleanName("x".repeat(80)).length)
    }

    @Test
    fun `the default name is the lowest free number`() {
        assertEquals("Profile 1", Profiles.defaultName(emptyList()))
        assertEquals("Profile 3", Profiles.defaultName(listOf(profile("a", "Profile 1"), profile("b", "Profile 2"))))
        assertEquals("Profile 1", Profiles.defaultName(listOf(profile("a", "Profile 2"))))
    }

    @Test
    fun `profiles survive encode and decode, and broken lines are left out`() {
        val list = listOf(profile("1", "Mum"), profile("2"))
        assertEquals(list, Profiles.decode(Profiles.encode(list)))
        assertEquals(emptyList(), Profiles.decode(""))
        assertEquals(list.take(1), Profiles.decode(Profiles.encode(list.take(1)) + "\nbroken line\nid\tname\tlinked:9"))
    }
}
