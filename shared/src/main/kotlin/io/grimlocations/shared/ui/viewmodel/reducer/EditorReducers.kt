package io.grimlocations.shared.ui.viewmodel.reducer

import io.grimlocations.shared.data.dto.LocationDTO
import io.grimlocations.shared.data.dto.firstContainer
import io.grimlocations.shared.data.repo.action.detectAndCreateProfilesAsync
import io.grimlocations.shared.data.repo.action.getLocationsAsync
import io.grimlocations.shared.data.repo.action.getMetaAsync
import io.grimlocations.shared.data.repo.action.getProfilesModsDifficultiesAsync
import io.grimlocations.shared.data.repo.createLocationsFromFile
import io.grimlocations.shared.data.repo.getFileLastModified
import io.grimlocations.shared.data.repo.isGDRunning
import io.grimlocations.shared.data.repo.writeLocationsToFile
import io.grimlocations.shared.framework.ui.getState
import io.grimlocations.shared.framework.ui.setState
import io.grimlocations.shared.framework.util.awaitAll
import io.grimlocations.shared.framework.util.extension.endsWithOne
import io.grimlocations.shared.framework.util.guardLet
import io.grimlocations.shared.ui.GLStateManager
import io.grimlocations.shared.ui.viewmodel.state.EditorState
import io.grimlocations.shared.ui.viewmodel.state.container.PMDContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


@Suppress("UNCHECKED_CAST")
suspend fun GLStateManager.loadEditorState(
    previousState: EditorState? = null
) {
    val (pmdMap, meta) = awaitAll(
        repository.getProfilesModsDifficultiesAsync(),
        repository.getMetaAsync()
    )

    if (previousState == null) {
        val container = pmdMap.firstContainer()
        val locList = repository.getLocationsAsync(container).await()

        setState(
            EditorState(
                profileMap = pmdMap,
                selectedPMDLeft = container,
                selectedPMDRight = container.copy(),
                locationsLeft = locList,
                locationsRight = locList.toSet(), //copies the list,
                activePMD = meta.activePMD,
                isGDRunning = false,
                locationsFileLastModified = null,
                selectedLocationsLeft = setOf(),
                selectedLocationsRight = setOf()
            )
        )
    } else {
        val (locList1, locList2) = awaitAll(
            repository.getLocationsAsync(previousState.selectedPMDLeft),
            repository.getLocationsAsync(previousState.selectedPMDRight)
        )

        setState(
            previousState.copy(
                profileMap = pmdMap,
                locationsLeft = locList1,
                locationsRight = locList2,
                activePMD = meta.activePMD,
            )
        )
    }
}

suspend fun GLStateManager.loadCharacterProfiles() {
    repository.detectAndCreateProfilesAsync().await()
    reloadEditorState()
}

suspend fun GLStateManager.reloadEditorState() {
    loadEditorState(getState())
}

suspend fun GLStateManager.updateIfGDRunning(): Boolean {
    val isRunning = repository.isGDRunning()
    val s = getState<EditorState>()

    if (s.isGDRunning != isRunning) {
        withContext(Dispatchers.Main) {
            setState(s.copy(isGDRunning = isRunning))
        }
    }
    return isRunning
}

suspend fun GLStateManager.checkIfLocationsFileChangedAndLoadLocation() {
    val meta = repository.getMetaAsync().await()
    guardLet(meta.installLocation, meta.activePMD) { loc, pmd ->
        val s = getState<EditorState>()
        val filePath = if (loc.endsWithOne("/", "\\"))
            loc + "GrimInternals_TeleportList.txt"
        else
            loc + File.separator + "GrimInternals_TeleportList.txt"

        val lastModified = repository.getFileLastModified(filePath)
        if (lastModified != null && s.locationsFileLastModified != lastModified) {
            repository.createLocationsFromFile(File(filePath), pmd)
            withContext(Dispatchers.Main) {
                loadEditorState(s.copy(locationsFileLastModified = lastModified))
            }
        }
    }
}

suspend fun GLStateManager.selectLocationsLeft(loc: Set<LocationDTO>) {
    val s = getState<EditorState>()
    withContext(Dispatchers.Main) {
        setState(s.copy(
            selectedLocationsLeft = loc
        ))
    }
}

suspend fun GLStateManager.selectLocationsRight(loc: Set<LocationDTO>) {
    val s = getState<EditorState>()
    withContext(Dispatchers.Main) {
        setState(s.copy(
            selectedLocationsRight = loc
        ))
    }
}

//suspend fun GLStateManager.deselectLocationsLeft(loc: Set<LocationDTO>) {
//    val s = getState<EditorState>()
//    withContext(Dispatchers.Main) {
//        setState(s.copy(
//            selectedLocationsLeft = s.selectedLocationsLeft.toMutableSet().apply { removeAll(loc) }
//        ))
//    }
//}
//
//suspend fun GLStateManager.deselectLocationsRight(loc: Set<LocationDTO>) {
//    val s = getState<EditorState>()
//    withContext(Dispatchers.Main) {
//        setState(s.copy(
//            selectedLocationsRight = s.selectedLocationsRight.toMutableSet().apply { removeAll(loc) }
//        ))
//    }
//}

suspend fun GLStateManager.selectPMDLeft(pmd: PMDContainer) {
    val s = getState<EditorState>()
    val locs = repository.getLocationsAsync(pmd).await()
    withContext(Dispatchers.Main) {
        setState(s.copy(
            selectedPMDLeft = pmd,
            locationsLeft = locs,
            selectedLocationsLeft = setOf()
        ))
    }
}

suspend fun GLStateManager.selectPMDRight(pmd: PMDContainer) {
    val s = getState<EditorState>()
    val locs = repository.getLocationsAsync(pmd).await()
    withContext(Dispatchers.Main) {
        setState(s.copy(
            selectedPMDRight = pmd,
            locationsRight = locs,
            selectedLocationsRight = setOf()
        ))
    }
}