package io.github.vlukyanets.lemonflow.data.system

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.vlukyanets.lemonflow.domain.model.AppLanguage
import io.github.vlukyanets.lemonflow.domain.port.LanguageSetting
import javax.inject.Inject
import javax.inject.Singleton

/** Android's per-app language (API 33+); the system persists it across restarts. */
@Singleton
class SystemLanguageSetting @Inject constructor(@ApplicationContext context: Context) :
    LanguageSetting {
    private val locales = context.getSystemService(LocaleManager::class.java)

    override fun current(): AppLanguage {
        val list = locales.applicationLocales
        return if (list.isEmpty) {
            AppLanguage.SYSTEM
        } else {
            AppLanguage.fromTag(
                list[0].toLanguageTag()
            )
        }
    }

    override fun set(language: AppLanguage) {
        locales.applicationLocales = language.tag
            ?.let { LocaleList.forLanguageTags(it) }
            ?: LocaleList.getEmptyLocaleList()
    }
}
