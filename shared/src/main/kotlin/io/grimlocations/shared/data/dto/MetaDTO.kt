package io.grimlocations.shared.data.dto

import io.grimlocations.shared.framework.data.dto.DTO
import io.grimlocations.shared.ui.viewmodel.state.container.PMDContainer
import java.time.LocalDateTime

data class MetaDTO(
    override val id: Int,
    override val created: LocalDateTime,
    override val modified: LocalDateTime,
    val version: Int,
    val saveLocation: String?,
    val installLocation: String?,
    val activePMD: PMDContainer?
) : DTO