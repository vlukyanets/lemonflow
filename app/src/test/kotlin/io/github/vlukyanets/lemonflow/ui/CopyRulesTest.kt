package io.github.vlukyanets.lemonflow.ui

import org.junit.Assert.assertTrue
import org.junit.Test

/** FR-016 / constitution V: no blaming or shaming words in any locale. */
class CopyRulesTest {
    private val banned = mapOf(
        "values" to listOf("failed", "missed", "broken", "streak lost"),
        "values-ru" to listOf("провал", "пропущ", "пропуст", "сорвал"),
        "values-uk" to listOf("провал", "пропущ", "пропуст", "зірвал")
    )

    @Test
    fun noBannedWordsInAnyLocale() {
        val hits = banned.flatMap { (folder, words) ->
            readStrings(folder).flatMap { resource ->
                resource.texts.flatMap { text ->
                    words.filter { text.contains(it, ignoreCase = true) }
                        .map { word -> "$folder/${resource.name}: '$word' in \"$text\"" }
                }
            }
        }
        assertTrue("Blaming wording found:\n" + hits.joinToString("\n"), hits.isEmpty())
    }
}
