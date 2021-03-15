package io.grimlocations.shared.ui.viewmodel.state

import io.grimlocations.shared.data.dto.ProfileModDifficultyMap
import io.grimlocations.shared.framework.ui.State
import io.grimlocations.shared.ui.viewmodel.state.container.PMDContainer

data class LauncherState(
    val map: ProfileModDifficultyMap,
    val selected: PMDContainer,
    val installPath: String,
    val locationsFilePath: String?,
) : State {
    override fun equals(other: Any?): Boolean {
        return this === other
    }
}