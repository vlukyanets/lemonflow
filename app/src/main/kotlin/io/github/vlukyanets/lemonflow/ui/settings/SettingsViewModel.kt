package io.github.vlukyanets.lemonflow.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vlukyanets.lemonflow.domain.model.AppLanguage
import io.github.vlukyanets.lemonflow.domain.port.LanguageSetting
import io.github.vlukyanets.lemonflow.domain.port.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SettingsSheet { DAY_END, LANGUAGE }

/** [dayEndHour] is null until the stored value has loaded; [sheet] is the open picker. */
data class SettingsUiState(
    val dayEndHour: Int? = null,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val sheet: SettingsSheet? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val languages: LanguageSetting
) : ViewModel() {
    private val sheet = MutableStateFlow<SettingsSheet?>(null)
    private val language = MutableStateFlow(languages.current())

    val state: StateFlow<SettingsUiState> =
        combine(settings.observeDayEndHour(), language, sheet) { hour, lang, open ->
            SettingsUiState(hour, lang, open)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            SettingsUiState(language = language.value)
        )

    fun open(which: SettingsSheet) {
        sheet.value = which
    }

    fun close() {
        sheet.value = null
    }

    fun pickHour(hour: Int) {
        sheet.value = null
        viewModelScope.launch { settings.setDayEndHour(hour) }
    }

    /**
     * Re-reads the language: it can also change outside the app (the system's per-app
     * language screen), and this ViewModel outlives the activity recreation that follows.
     */
    fun refreshLanguage() {
        language.value = languages.current()
    }

    /** The system recreates the activity in the new language; this ViewModel survives it. */
    fun pickLanguage(choice: AppLanguage) {
        sheet.value = null
        if (choice == language.value) return
        language.value = choice
        languages.set(choice)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
