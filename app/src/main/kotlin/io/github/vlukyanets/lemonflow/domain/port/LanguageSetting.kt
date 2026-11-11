package io.github.vlukyanets.lemonflow.domain.port

import io.github.vlukyanets.lemonflow.domain.model.AppLanguage

/** The per-app language kept by the system, not by our database (research R4). */
interface LanguageSetting {
    fun current(): AppLanguage

    /** Applies [language] to the whole app; the system recreates visible activities. */
    fun set(language: AppLanguage)
}
