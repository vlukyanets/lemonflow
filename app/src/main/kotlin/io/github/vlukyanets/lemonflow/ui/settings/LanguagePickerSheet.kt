package io.github.vlukyanets.lemonflow.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.AppLanguage
import io.github.vlukyanets.lemonflow.ui.common.LemonSheet
import io.github.vlukyanets.lemonflow.ui.common.SheetHeader
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/** App language as a radio group; languages are listed by their own names (FR-022). */
@Composable
internal fun LanguagePickerSheet(
    selected: AppLanguage,
    onPick: (AppLanguage) -> Unit,
    onClose: () -> Unit
) {
    LemonSheet(onDismiss = onClose) {
        SheetHeader(
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(R.string.settings_language_hint),
            onClose = onClose
        )
        Column(Modifier.selectableGroup()) {
            AppLanguage.entries.forEach { language ->
                HorizontalDivider(color = LemonFlow.colors.surface2)
                PickerOption(stringResource(languageName(language)), language == selected) {
                    onPick(language)
                }
            }
        }
    }
}

@StringRes
internal fun languageName(language: AppLanguage): Int = when (language) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.ENGLISH -> R.string.language_english
    AppLanguage.RUSSIAN -> R.string.language_russian
    AppLanguage.UKRAINIAN -> R.string.language_ukrainian
}
