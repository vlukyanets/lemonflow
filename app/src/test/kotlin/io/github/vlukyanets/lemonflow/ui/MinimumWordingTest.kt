package io.github.vlukyanets.lemonflow.ui

import org.junit.Assert.assertFalse
import org.junit.Test

class MinimumWordingTest {
    private val minimumStrings =
        listOf("today_min_tag", "today_min_amount", "goal_target_min")

    @Test
    fun englishMinimumIsNotTheMinutesAbbreviation() = assertDistinct("values")

    @Test
    fun russianMinimumIsNotTheMinutesAbbreviation() = assertDistinct("values-ru")

    @Test
    fun ukrainianMinimumIsNotTheMinutesAbbreviation() = assertDistinct("values-uk")

    private fun assertDistinct(folder: String) {
        val strings = readStrings(folder)
            .filterNot { it.isPlural }
            .associate { it.name to it.texts.single() }
        val minutes = strings.getValue("unit_min_short")
        // The abbreviation as a whole word, optionally followed by a dot ("мин.").
        val word = Regex("(^|[\\s·])" + Regex.escape(minutes) + "\\.?($|[\\s·%])")
        minimumStrings.forEach { name ->
            // Placeholders like %1$s stand for amounts; drop them before matching.
            val text = strings.getValue(name).replace(Regex("%\\d.s"), "X")
            // goal_target_min keeps minutes only inside the %s amounts, never as a label.
            assertFalse("$folder/$name uses '$minutes': $text", word.containsMatchIn(text))
        }
    }
}
