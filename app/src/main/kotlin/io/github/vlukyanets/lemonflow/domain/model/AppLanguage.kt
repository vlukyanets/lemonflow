package io.github.vlukyanets.lemonflow.domain.model

/** The app's UI language (FR-022); [tag] is a BCP 47 code, null for [SYSTEM]. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    RUSSIAN("ru"),
    UKRAINIAN("uk");

    companion object {
        /** Maps a stored language tag such as "ru" or "uk-UA"; unknown tags follow the system. */
        fun fromTag(tag: String?): AppLanguage {
            val language = tag?.substringBefore('-')?.lowercase()
            return entries.firstOrNull { it.tag != null && it.tag == language } ?: SYSTEM
        }
    }
}
