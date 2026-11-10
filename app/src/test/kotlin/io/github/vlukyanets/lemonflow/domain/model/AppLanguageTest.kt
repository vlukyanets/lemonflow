package io.github.vlukyanets.lemonflow.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun knownTagsMapToTheirLanguage() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromTag("ru"))
        assertEquals(AppLanguage.UKRAINIAN, AppLanguage.fromTag("uk"))
    }

    @Test
    fun regionAndCaseAreIgnored() {
        assertEquals(AppLanguage.UKRAINIAN, AppLanguage.fromTag("uk-UA"))
        assertEquals(AppLanguage.RUSSIAN, AppLanguage.fromTag("RU"))
    }

    @Test
    fun unknownOrMissingTagsFollowTheSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("de"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
    }
}
