package io.grimlocations.data.domain

import io.grimlocations.data.dto.ModDTO
import io.grimlocations.framework.data.domain.BaseTable
import io.grimlocations.framework.data.domain.DTOEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

object ModTable : BaseTable("mod") {
    val name = text("name").uniqueIndex()
}

class Mod(id: EntityID<Int>) : DTOEntity<ModTable, ModDTO>(id, ModTable) {
    var name by ModTable.name

    var profiles by Profile via ProfileModIntermTable
    var difficulties by Difficulty via ModDifficultyIntermTable
    val locations by Location referrersOn LocationTable.mod

    override fun toDTO(): ModDTO {
        return ModDTO(id.value, created, modified, name)
    }

    companion object : IntEntityClass<Mod>(ModTable)
}