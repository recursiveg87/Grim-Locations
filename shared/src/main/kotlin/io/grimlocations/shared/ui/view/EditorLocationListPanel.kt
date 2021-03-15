package io.grimlocations.shared.ui.view

import androidx.compose.desktop.AppWindow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.grimlocations.shared.data.dto.LocationDTO
import io.grimlocations.shared.data.dto.RESERVED_PROFILES
import io.grimlocations.shared.ui.view.component.LocationListComponent
import io.grimlocations.shared.ui.view.component.PMDChooserComponent
import io.grimlocations.shared.ui.viewmodel.EditorViewModel
import io.grimlocations.shared.ui.viewmodel.event.*
import io.grimlocations.shared.ui.viewmodel.state.EditorState
import io.grimlocations.shared.ui.viewmodel.state.container.PMDContainer

private val listHeight = 400.dp
private val arrowButtonSize = 50.dp
private val rowHeight = 50.dp
private val rowWidth = 550.dp
private val horizontalSpacerWidth = 10.dp
private val verticalSpacerHeight = 20.dp

private var previousPMDLeft: PMDContainer? = null
private var previousPMDRight: PMDContainer? = null

private lateinit var stateVerticalLeft: LazyListState
private lateinit var stateVerticalRight: LazyListState

@ExperimentalFoundationApi
@Composable
fun EditorLocationListPanel(
    state: EditorState,
    vm: EditorViewModel,
    onOpen: (AppWindow?, AppWindow) -> Unit,
    onClose: (() -> Unit),
) {
    with(state) {
        val isLeftArrowDisabled = isArrowLeftRightDisabled(
            primaryPMD = selectedPMDLeft,
            otherPMD = selectedPMDRight,
            primarySelectedLocations = selectedLocationsLeft,
            otherLocations = locationsRight,
        )
        val isRightArrowDisabled = isArrowLeftRightDisabled(
            primaryPMD = selectedPMDRight,
            otherPMD = selectedPMDLeft,
            primarySelectedLocations = selectedLocationsRight,
            otherLocations = locationsLeft,
        )
        if (previousPMDLeft != selectedPMDLeft) {
            stateVerticalLeft = LazyListState(
                0,
                0
            )
            previousPMDLeft = selectedPMDLeft
        }
        if (previousPMDRight != selectedPMDRight) {
            stateVerticalRight = LazyListState(
                0,
                0
            )
            previousPMDRight = selectedPMDRight
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    PMDChooserComponent(
                        map = profileMap,
                        selected = selectedPMDLeft,
                        onOpen = onOpen,
                        onClose = onClose,
                        onSelect = { pmdContainer ->
                            vm.selectPMDLeft(pmdContainer)
                        }
                    )
                }
                Spacer(Modifier.height(verticalSpacerHeight))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(listHeight)
                ) {
                    LocationListComponent(
                        rowHeight = rowHeight,
                        rowWidth = rowWidth,
                        locations = locationsLeft,
                        selectedLocations = selectedLocationsLeft,
                        onSelectLocations = { locs ->
                            vm.selectLocationsLeft(locs)
                        },
                        stateVertical = stateVerticalLeft
                    )
                    Spacer(Modifier.width(horizontalSpacerWidth))
                    Column {
                        ArrowUpButton(
                            pmdContainer = selectedPMDLeft,
                            locations = locationsLeft,
                            selected = selectedLocationsLeft,
                            onClick = {
                                vm.moveSelectedLeftUp()
                            }
                        )
                        ArrowLeftRightButton(
                            isLeft = true,
                            disabled = isLeftArrowDisabled,
                            onClick = {
                                vm.copyLeftSelectedToRight()
                            }
                        )
                        ArrowDownButton(
                            pmdContainer = selectedPMDLeft,
                            locations = locationsLeft,
                            selected = selectedLocationsLeft,
                            onClick = {
                                vm.moveSelectedLeftDown()
                            }
                        )
                    }

                }
            }
            Spacer(Modifier.width(15.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    PMDChooserComponent(
                        map = profileMap,
                        selected = selectedPMDRight,
                        onOpen = onOpen,
                        onClose = onClose,
                        onSelect = { pmdContainer ->
                            vm.selectPMDRight(pmdContainer)
                        }
                    )
                }
                Spacer(Modifier.height(verticalSpacerHeight))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(listHeight)
                ) {
                    Column {
                        ArrowUpButton(
                            pmdContainer = selectedPMDRight,
                            locations = locationsRight,
                            selected = selectedLocationsRight,
                            onClick = {
                                vm.moveSelectedRightUp()
                            }
                        )
                        ArrowLeftRightButton(
                            isLeft = false,
                            disabled = isRightArrowDisabled,
                            onClick = {
                                vm.copyRightSelectedToLeft()
                            }
                        )
                        ArrowDownButton(
                            pmdContainer = selectedPMDRight,
                            locations = locationsRight,
                            selected = selectedLocationsRight,
                            onClick = {
                                vm.moveSelectedRightDown()
                            }
                        )
                    }

                    Spacer(Modifier.width(horizontalSpacerWidth))
                    LocationListComponent(
                        rowHeight = rowHeight,
                        rowWidth = rowWidth,
                        locations = locationsRight,
                        selectedLocations = selectedLocationsRight,
                        onSelectLocations = { locs ->
                            vm.selectLocationsRight(locs)
                        },
                        stateVertical = stateVerticalRight
                    )
                }
            }
        }
    }
}

@Composable
private fun ArrowLeftRightButton(isLeft: Boolean, disabled: Boolean, onClick: () -> Unit) {
    IconButton(
        modifier = Modifier.size(arrowButtonSize),
        enabled = !disabled,
        onClick = onClick,
    ) {
        Icon(
            if (isLeft) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
            "Copy",
            tint = if (disabled) Color.DarkGray else MaterialTheme.colors.primary
        )
    }
}

@Composable
private fun ArrowUpButton(
    pmdContainer: PMDContainer,
    locations: Set<LocationDTO>,
    selected: Set<LocationDTO>,
    onClick: () -> Unit
) {
    val disabled = selected.isEmpty() || locations.isEmpty() ||
            locations.first() == selected.first() || RESERVED_PROFILES.contains(pmdContainer.profile)

    IconButton(
        modifier = Modifier.size(arrowButtonSize),
        enabled = !disabled,
        onClick = onClick,
    ) {
        Icon(
            Icons.Default.KeyboardArrowUp,
            "Copy",
            tint = if (disabled) Color.DarkGray else MaterialTheme.colors.primary
        )
    }
}

@Composable
private fun ArrowDownButton(
    pmdContainer: PMDContainer,
    locations: Set<LocationDTO>,
    selected: Set<LocationDTO>,
    onClick: () -> Unit
) {
    val disabled = selected.isEmpty() || locations.isEmpty() ||
            locations.last() == selected.last() || RESERVED_PROFILES.contains(pmdContainer.profile)

    IconButton(
        modifier = Modifier.size(arrowButtonSize),
        enabled = !disabled,
        onClick = onClick,
    ) {
        Icon(
            Icons.Default.KeyboardArrowDown,
            "Copy",
            tint = if (disabled) Color.DarkGray else MaterialTheme.colors.primary
        )
    }
}

private fun SelectRangeButton(onClick: () -> Unit) {

}

private fun isArrowLeftRightDisabled(
    primaryPMD: PMDContainer,
    primarySelectedLocations: Set<LocationDTO>,
    otherPMD: PMDContainer,
    otherLocations: Set<LocationDTO>,
): Boolean {
    if (primaryPMD == otherPMD)
        return true

    if (RESERVED_PROFILES.contains(otherPMD.profile))
        return true

    if(primarySelectedLocations.isEmpty())
        return true

    primarySelectedLocations.forEach {
        if (otherLocations.contains(it))
            return true
    }

    return false
}