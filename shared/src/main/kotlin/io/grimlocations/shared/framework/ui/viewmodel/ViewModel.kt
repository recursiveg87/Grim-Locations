package io.grimlocations.shared.framework.ui.viewmodel

import io.grimlocations.shared.framework.ui.State
import io.grimlocations.shared.framework.ui.StateManager
import io.grimlocations.shared.framework.ui.getMutableFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

abstract class ViewModel<T: State, M: StateManager<*>> {
    val viewModelJob: Job = Job()
    val viewModelScope: CoroutineScope = CoroutineScope(Dispatchers.Default + viewModelJob)

    abstract val stateManager: M

    abstract fun loadState()
}

inline val <reified T: State, M: StateManager<*>> ViewModel<T, M>.stateFlow: StateFlow<T?>
    get() = stateManager.getMutableFlow()