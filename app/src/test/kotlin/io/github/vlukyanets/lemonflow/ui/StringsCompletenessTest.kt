package io.github.vlukyanets.lemonflow.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/** Constitution VII: every string and plural exists in English, Russian and Ukrainian. */
class StringsCompletenessTest {
    private val english = readStrings("values")

    @Test
    fun russianHasEveryString() = assertComplete("values-ru")

    @Test
    fun ukrainianHasEveryString() = assertComplete("values-uk")

    private fun assertComplete(folder: String) {
        val translated = readStrings(folder).associateBy { it.name }
        val missing = english.filter { resource ->
            val other = translated[resource.name]
            other == null || other.isPlural != resource.isPlural
        }
        assertTrue(
            "$folder lacks: ${missing.joinToString { it.name }}",
            missing.isEmpty()
        )
    }
}
