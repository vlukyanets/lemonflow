package io.github.vlukyanets.lemonflow.testing

import io.github.vlukyanets.lemonflow.domain.model.AppLanguage
import io.github.vlukyanets.lemonflow.domain.port.LanguageSetting

/** Remembers the choice instead of asking the system to recreate the activity. */
class FakeLanguageSetting(var language: AppLanguage = AppLanguage.SYSTEM) : LanguageSetting {
    var setCalls = 0

    override fun current(): AppLanguage = language

    override fun set(language: AppLanguage) {
        setCalls++
        this.language = language
    }
}
