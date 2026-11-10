package io.github.vlukyanets.lemonflow.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.port.HabitRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitsUiState(val active: List<Habit> = emptyList(), val archivedCount: Int = 0)

@HiltViewModel
class HabitsViewModel @Inject constructor(repository: HabitRepository) : ViewModel() {
    val state: StateFlow<HabitsUiState> =
        combine(repository.observeActive(), repository.observeArchived()) { active, archived ->
            HabitsUiState(active, archived.size)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HabitsUiState())
}

data class ArchiveUiState(val archived: List<Habit> = emptyList(), val pendingDelete: Habit? = null)

@HiltViewModel
class ArchiveViewModel @Inject constructor(private val repository: HabitRepository) : ViewModel() {
    private val pendingDelete = MutableStateFlow<Habit?>(null)

    val state: StateFlow<ArchiveUiState> =
        combine(repository.observeArchived(), pendingDelete.asStateFlow()) { archived, pending ->
            ArchiveUiState(archived, pending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ArchiveUiState())

    fun restore(id: HabitId) {
        viewModelScope.launch { repository.restore(id) }
    }

    fun askDelete(habit: Habit?) {
        pendingDelete.value = habit
    }

    fun confirmDelete() {
        val habit = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch { repository.deletePermanently(habit.id) }
    }
}

private const val STOP_TIMEOUT_MS = 5_000L
