package com.calias.tv.core.profile

/** One person's profile on this TV: who it is and where its playlist comes from. Its viewing state is stored under [id]. */
data class Profile(val id: String, val name: String, val source: Source)

/** The list of profiles on one TV: its limit, names, and how it is stored. */
object Profiles {

    const val MAX = 5
    private const val MAX_NAME = 30
    private const val SEPARATOR = '\t'

    fun isFull(profiles: List<Profile>) = profiles.size >= MAX

    /** [profiles] with [profile] added at the end. */
    fun add(profiles: List<Profile>, profile: Profile): List<Profile> {
        require(!isFull(profiles)) { "At most $MAX profiles" }
        return profiles + profile
    }

    /** [profiles] with the profile of the same id replaced by [profile]. */
    fun replace(profiles: List<Profile>, profile: Profile): List<Profile> =
        profiles.map { if (it.id == profile.id) profile else it }

    fun remove(profiles: List<Profile>, id: String): List<Profile> = profiles.filter { it.id != id }

    /** What the user typed as a name: trimmed, runs of white space (tabs, new lines) become one space, cut to a length that fits a row. */
    fun cleanName(typed: String): String = typed.trim().replace(Regex("\\s+"), " ").take(MAX_NAME).trimEnd()

    /** "Profile 1", or the lowest number not yet in use. */
    fun defaultName(profiles: List<Profile>): String {
        val taken = profiles.map { it.name }.toSet()
        return generateSequence(1) { it + 1 }.map { "Profile $it" }.first { it !in taken }
    }

    /** One profile per line: id, name, source, separated by a tab. Names and sources never hold a tab or a new line. */
    fun encode(profiles: List<Profile>): String =
        profiles.joinToString("\n") { listOf(it.id, it.name, Source.encode(it.source)).joinToString(SEPARATOR.toString()) }

    /** Reads [encode] output. Lines that cannot be read are left out. */
    fun decode(text: String): List<Profile> = text.lines().mapNotNull { line ->
        val parts = line.split(SEPARATOR)
        val source = parts.getOrNull(2)?.let(Source::decode)
        if (parts.size == 3 && source != null) Profile(parts[0], parts[1], source) else null
    }
}
