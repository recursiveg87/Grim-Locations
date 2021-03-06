package io.grimlocations.ui.view.component

import androidx.compose.desktop.AppWindow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.grimlocations.data.dto.*
import io.grimlocations.ui.viewmodel.state.container.PMDContainer
import io.grimlocations.util.extension.closeIfOpen

private val textBoxWidth = 300.dp

@ExperimentalFoundationApi
@Composable
fun PMDChooserComponent(
    map: ProfileModDifficultyMap,
    selected: PMDContainer,
    onSelect: (PMDContainer) -> Unit,
    onOpen: (AppWindow?, AppWindow) -> Unit,
    onClose: (() -> Unit)?,
    spacerHeight: Dp = 15.dp,
) {
    val profileListWindow = remember { mutableStateOf<AppWindow?>(null) }
    val modListWindow = remember { mutableStateOf<AppWindow?>(null) }
    val difficultyListWindow = remember { mutableStateOf<AppWindow?>(null) }

    val primaryColor = MaterialTheme.colors.primary
    val profiles = remember(map) { map.keys.map { Triple(it.id, it.name, if (it.isReserved) primaryColor else null ) } }
    val mods = remember(map, selected) { map[selected.profile]!!.keys.map { Triple(it.id, it.name, null) } }
    val difficulties = remember(map, selected) { map[selected.profile]!![selected.mod]!!.map { Triple(it.id, it.name, null) } }

    val selectedProfile = selected.profile
    val selectedMod =
        if (selected.mod == RESERVED_NO_MODS_INDICATOR)
            selected.mod.copy(name = "None")
        else
            selected.mod
    val selectedDifficulty =
        if (selected.difficulty == RESERVED_NO_DIFFICULTIES_INDICATOR)
            selected.difficulty.copy(name = "None")
        else
            selected.difficulty

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ComboPopup(
            "Profile",
            items = profiles,
            emptyItemsMessage = "No Profiles",
            selected = Triple(selectedProfile.id, selectedProfile.name, if (selectedProfile.isReserved) primaryColor else null),
            width = textBoxWidth,
            onOpen = { p, c ->
                manageWindows(
                    onOpen, p, c,
                    profileListWindow, modListWindow, difficultyListWindow
                )
            },
            onClose = onClose,
            onSelect = {
                val profile = map.keys.find { item -> item.id == it.first }!!
                val mod = map[profile]!!.keys.first()
                val difficulty = map[profile]!![mod]!!.first()

                onSelect(
                    PMDContainer(
                        profile = profile,
                        mod = mod,
                        difficulty = difficulty
                    )
                )
            }
        )
        Spacer(modifier = Modifier.height(spacerHeight))
        ComboPopup(
            "Mod",
            items = mods,
            emptyItemsMessage = "No Mods",
            disabled = selected.mod == RESERVED_NO_MODS_INDICATOR,
            selected = Triple(selectedMod.id, selectedMod.name, null),
            width = textBoxWidth,
            onOpen = { p, c ->
                manageWindows(
                    onOpen, p, c,
                    modListWindow, profileListWindow, difficultyListWindow
                )
            },
            onClose = onClose,
            onSelect = {
                val mod = map[selected.profile]!!.keys.find { item -> item.id == it.first }!!
                val difficulty = map[selected.profile]!![mod]!!.first()

                onSelect(
                    PMDContainer(
                        profile = selected.profile,
                        mod = mod,
                        difficulty = difficulty
                    )
                )
            }
        )
        Spacer(modifier = Modifier.height(spacerHeight))
        ComboPopup(
            "Difficulty",
            items = difficulties,
            emptyItemsMessage = "No Difficulties",
            disabled = selected.difficulty == RESERVED_NO_DIFFICULTIES_INDICATOR,
            selected = Triple(selectedDifficulty.id, selectedDifficulty.name, null),
            width = textBoxWidth,
            onOpen = { p, c ->
                manageWindows(
                    onOpen, p, c,
                    difficultyListWindow, modListWindow, profileListWindow
                )
            },
            onClose = onClose,
            onSelect = {
                val difficulty = map[selected.profile]!![selected.mod]!!.find { item -> item.id == it.first }!!

                onSelect(
                    PMDContainer(
                        profile = selected.profile,
                        mod = selected.mod,
                        difficulty = difficulty
                    )
                )
            }
        )
    }
}

private fun manageWindows(
    onOpen: (AppWindow?, AppWindow) -> Unit,
    p: AppWindow?,
    c: AppWindow,
    focus: MutableState<AppWindow?>,
    other1: MutableState<AppWindow?>,
    other2: MutableState<AppWindow?>,
) {
    focus.value = c
    other1.value?.closeIfOpen()
    other2.value?.closeIfOpen()
    onOpen(p, c) //must do as last line of this function
}
