package io.grimlocations.data.repo.action

import io.grimlocations.data.domain.MetaTable
import io.grimlocations.data.domain.Mod
import io.grimlocations.data.domain.Profile
import io.grimlocations.data.domain.ProfileTable
import io.grimlocations.data.dto.*
import io.grimlocations.data.repo.SqliteRepository
import io.grimlocations.framework.data.dto.containsId
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import java.io.File

private val logger = LogManager.getLogger()

suspend fun SqliteRepository.getProfilesAsync(): Deferred<List<ProfileDTO>> =
    suspendedTransactionAsync(Dispatchers.IO) {
        Profile.wrapRows(ProfileTable.selectAll()).map { it.toDTO() }
    }

suspend fun SqliteRepository.getProfilesModsDifficultiesAsync(
    includeReservedProfiles: Boolean = true
): Deferred<ProfileModDifficultyMap> =
    suspendedTransactionAsync(Dispatchers.IO) {
        val map: MutableProfileModDifficultyMap = mutableMapOf()
        val reservedMap: MutableProfileModDifficultyMap = mutableMapOf()
        val regularMap: MutableProfileModDifficultyMap = mutableMapOf()

        ProfileTable.selectAll().forEach {
            val p = Profile.wrapRow(it)
            if (includeReservedProfiles && RESERVED_PROFILES.containsId(p.id.value)) {
                val mmap: MutableModDifficultyMap = mutableMapOf()

                p.mods.forEach { m ->
                    mmap[m.toDTO()] = m.difficulties.map { d -> d.toDTO() } as MutableList<DifficultyDTO>
                }
                reservedMap[p.toDTO()] = mmap
            } else if (!RESERVED_PROFILES.containsId(p.id.value)) {
                val mmap: MutableModDifficultyMap = mutableMapOf()

                p.mods.forEach { m ->
                    mmap[m.toDTO()] = m.difficulties.map { d -> d.toDTO() } as MutableList<DifficultyDTO>
                }
                regularMap[p.toDTO()] = mmap
            }
        }
        map.putAll(reservedMap)
        map.putAll(regularMap)
        map
    }

suspend fun SqliteRepository.detectAndCreateProfilesAsync(): Deferred<Unit> =
    withContext(Dispatchers.IO) {
        async<Unit> {
            val path = newSuspendedTransaction {
                MetaTable.slice(MetaTable.saveLocation).selectAll().single()[MetaTable.saveLocation]
            }

            try {
                File(path).listFiles { it: File -> it.isDirectory }?.also {
                    for (file in it) {
                        val n = file.name.trim().removePrefix("_")
                        if (n.isNotBlank()) {
                            try {
                                var p = newSuspendedTransaction {
                                    Profile.find { ProfileTable.name eq n }.singleOrNull()
                                }
                                if (p == null) {
                                    p = newSuspendedTransaction {
                                        Profile.new {
                                            name = n
                                        }
                                    }
                                    newSuspendedTransaction {
                                        val mod = Mod.findById(DEFAULT_GAME_MOD.id)!!
                                        p.mods = SizedCollection(listOf(mod))
                                    }
                                } else {
                                    logger.info("Duplicate profile found: $n")
                                }
                            } catch (e: Exception) {
                                logger.error("", e)
                            }
                        }
                    }
                } ?: run {
                    logger.error("Path is either not a directory or an I/O error has occurred.")
                }
            } catch (e: SecurityException) {
                logger.error("Read access is denied to this directory: $path", e)
            }
        }
    }