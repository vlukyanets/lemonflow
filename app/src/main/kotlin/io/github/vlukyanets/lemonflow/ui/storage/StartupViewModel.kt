package io.github.vlukyanets.lemonflow.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vlukyanets.lemonflow.domain.port.StorageGate
import io.github.vlukyanets.lemonflow.domain.port.StorageState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartupState { Loading, Ready, Unavailable }

/** Opens the encrypted store before any screen touches data (FR-015). */
@HiltViewModel
class StartupViewModel @Inject constructor(private val gate: StorageGate) : ViewModel() {
    private val _state = MutableStateFlow(StartupState.Loading)
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = gate.open().toStartup() }
    }

    /** Call only after the user confirmed in the dialog. */
    fun startFresh() {
        viewModelScope.launch {
            _state.value = StartupState.Loading
            _state.value = gate.startFresh().toStartup()
        }
    }

    private fun StorageState.toStartup(): StartupState = when (this) {
        StorageState.Ready -> StartupState.Ready
        StorageState.Unavailable -> StartupState.Unavailable
    }
}
