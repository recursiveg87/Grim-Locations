package io.grimlocations.shared.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import io.grimlocations.shared.framework.ui.viewmodel.ViewModel
import io.grimlocations.shared.ui.GLStateManager
import io.grimlocations.shared.ui.viewmodel.reducer.loadEditorState
import io.grimlocations.shared.ui.viewmodel.state.EditorState
import kotlinx.coroutines.launch

class EditorViewModel(override val stateManager: GLStateManager) :
    ViewModel<EditorState, GLStateManager>() {

    private val _isLeftMultiSelect = mutableStateOf(false)
    private val _isRightMultiSelect = mutableStateOf(false)

    var isLeftMultiSelect: Boolean
        get() = _isLeftMultiSelect.value
        set(value) {
            _isLeftMultiSelect.value = value
        }

    var isRightMultiSelect: Boolean
        get() = _isRightMultiSelect.value
        set(value) {
            _isRightMultiSelect.value = value
        }

    override fun loadState() {
        viewModelScope.launch {
            stateManager.loadEditorState()
        }
    }
}