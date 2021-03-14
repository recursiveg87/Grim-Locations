package io.grimlocations.shared.data.repo

import io.grimlocations.shared.data.domain.*
import io.grimlocations.shared.data.dto.*
import io.grimlocations.shared.data.repo.action.getMetaAsync
import io.grimlocations.shared.framework.ui.getState
import io.grimlocations.shared.framework.util.extension.removeAllBlank
import io.grimlocations.shared.ui.viewmodel.reducer.loadEditorState
import io.grimlocations.shared.ui.viewmodel.state.EditorState
import io.grimlocations.shared.ui.viewmodel.state.container.PMDContainer
import io.grimlocations.shared.util.extension.glDatabaseBackupDir
import io.grimlocations.shared.util.extension.glDatabaseDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.max
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger: Logger = LogManager.getLogger()

fun SqliteRepository.createRollingBackup(maxBackups: Int) {
    try {
        var backupNumber = 1
        val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val backupDirPath = appDirs.glDatabaseBackupDir
        val backupDir = File(backupDirPath)
        backupDir.mkdirs()
        val files = backupDir.listFiles { it: File -> it.isFile }!!

        if (files.isNotEmpty()) {
            val todaysFiles =
                files.filter { f -> f.name.contains(date) }.sortedByDescending { it.lastModified() }

            if (todaysFiles.isNotEmpty()) {
                val name = todaysFiles[0].name
                val start = name.lastIndexOf("-") + 1
                val end = name.lastIndexOf(".")
                backupNumber = name.substring(start, end).toInt() + 1
            }

            if (files.size == maxBackups) {
                files.sortedBy { it.lastModified() }[0].delete()
            }
        }

        val currentDb = File("${appDirs.glDatabaseDir + File.separator}database.db")
        currentDb.copyTo(File("$backupDirPath${File.separator}database-$date-$backupNumber.db"))
    } catch (e: Exception) {
        logger.error("Issue creating the rolling backup.", e)
        throw e
    }
}

//The string returned is the error string, if everything went well then it will return null
suspend fun SqliteRepository.createLocationsFromFile(
    file: File,
    pmd: PMDContainer
): String? = createLocationsFromFile(file, pmd.profile, pmd.mod, pmd.difficulty)

//The string returned is the error string, if everything went well then it will return null
suspend fun SqliteRepository.createLocationsFromFile(
    file: File,
    profileDTO: ProfileDTO,
    modDTO: ModDTO,
    difficultyDTO: DifficultyDTO
): String? {
    logger.info("Loading locations from ${file.name}")

    val locList = mutableListOf<LocationDTO>()
    val time = LocalDateTime.now()
    var errorString: String? = null

    file.forEachLine {
        if (errorString != null) //lazy man's way of breaking from the loop (performs a continue on every item when theres an error)
            return@forEachLine

        if (it.isNotBlank()) {
            val loc = it.split(",").removeAllBlank()
            if (loc.size != 4) {
                errorString =
                    "The csv file is not in the correct format. " +
                            "The required format for each line is Name,Coordinate1,Coordinate2,Coordinate3.\n" +
                            "The line in question is: $it"
                logger.error(errorString)
                return@forEachLine
            }

            val name = loc[0].trim()
            if (name.isBlank()) {
                errorString = "The name of the location cannot be blank.\n" +
                        "The line in question is: $it"
                logger.error(errorString)
                return@forEachLine
            }

            val coord1 = loc[1].trim()
            try {
                BigDecimal(coord1)
            } catch (e: Exception) {
                errorString = "The first coordinate is not a number.\n" +
                        "The line in question is: $it"
                logger.error(errorString, e)
                return@forEachLine
            }

            val coord2 = loc[2].trim()
            try {
                BigDecimal(coord2)
            } catch (e: Exception) {
                errorString = "The second coordinate is not a number.\n" +
                        "The line in question is: $it"
                logger.error(errorString, e)
                return@forEachLine
            }

            val coord3 = loc[3].trim()
            try {
                BigDecimal(coord3)
            } catch (e: Exception) {
                errorString = "The third coordinate is not a number.\n" +
                        "The line in question is: $it"
                logger.error(errorString, e)
                return@forEachLine
            }

            val coordDTO = CoordinateDTO(-1, time, time, coord1, coord2, coord3)
            locList.add(LocationDTO(-1, time, time, name, 0, coordDTO))
        }
    }

    if (errorString != null)
        return errorString

    //create the coordinate if it doesn't exist
    try {
        newSuspendedTransaction {
            locList.forEach {

                Coordinate.find {
                    (CoordinateTable.coordinate1 eq it.coordinate.coordinate1) and
                            (CoordinateTable.coordinate2 eq it.coordinate.coordinate2) and
                            (CoordinateTable.coordinate3 eq it.coordinate.coordinate3)
                }.singleOrNull() ?: Coordinate.new {
                    coordinate1 = it.coordinate.coordinate1
                    coordinate2 = it.coordinate.coordinate2
                    coordinate3 = it.coordinate.coordinate3
                }

            }
        }
    } catch (e: Exception) {
        errorString = "Issue creating coordinates for file ${file.name}"
        logger.error(errorString, e)
    }

    if (errorString != null)
        return errorString

    //create the locations (theres probably a better way to do all this)
    try {
        newSuspendedTransaction {
            val _profile = Profile.findById(profileDTO.id)!!
            val _mod = Mod.findById(modDTO.id)!!
            val _difficulty = Difficulty.findById(difficultyDTO.id)!!

            locList.forEach {
                val coord = Coordinate.find {
                    (CoordinateTable.coordinate1 eq it.coordinate.coordinate1) and
                            (CoordinateTable.coordinate2 eq it.coordinate.coordinate2) and
                            (CoordinateTable.coordinate3 eq it.coordinate.coordinate3)
                }.single()

                Location.find {
                    (LocationTable.profile eq _profile.id) and
                            (LocationTable.mod eq _mod.id) and
                            (LocationTable.difficulty eq _difficulty.id) and
                            (LocationTable.coordinate eq coord.id)
                }.singleOrNull() ?: run {
                    val o = LocationTable.slice(LocationTable.order).select {
                        (LocationTable.profile eq _profile.id) and
                                (LocationTable.mod eq _mod.id) and
                                (LocationTable.difficulty eq _difficulty.id)
                    }.map { it[LocationTable.order] }.maxOrNull() ?: 0

                    Location.new {
                        name = it.name
                        order = o + 1
                        profile = _profile
                        mod = _mod
                        difficulty = _difficulty
                        coordinate = coord
                    }
                }
            }
        }
    } catch (e: Exception) {
        errorString = "Issue creating locations for file ${file.name}"
        logger.error(errorString, e)
    }

    return errorString
}

//returns null if no errors, string if error
suspend fun SqliteRepository.writeLocationsToFile(filePath: String, pmd: PMDContainer) =
    withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.createNewFile()
            val locations = newSuspendedTransaction {
                Location.find {
                    (LocationTable.profile eq pmd.profile.id) and
                            (LocationTable.mod eq pmd.mod.id) and
                            (LocationTable.difficulty eq pmd.difficulty.id)
                }.map { it.toDTO() }
            }

            file.writeText(
                buildString {
                    locations.forEach {
                        with(it) {
                            val c = coordinate
                            append("$name, ${c.coordinate1}, ${c.coordinate2}, ${c.coordinate3},\n")
                        }
                    }
                }
            )
            null
        } catch (e: Exception) {
            val msg = "Error writing to file: $filePath"
            logger.error(msg, e)
            msg
        }
    }

suspend fun SqliteRepository.isGDRunning() = withContext(Dispatchers.IO) {
    lateinit var line: String
    var pidInfo = ""

    val p = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe")

    val input = BufferedReader(InputStreamReader(p.inputStream))

    while (input.readLine()?.also { line = it } != null) {
        pidInfo += line
    }

    input.close()
    pidInfo.contains("Grim Dawn.exe")
}

//returns null if problem opening file
suspend fun SqliteRepository.getFileLastModified(path: String): Long? = withContext(Dispatchers.IO) {
    try {
        File(path).lastModified()
    } catch (e: Exception) {
        logger.error("Problem loading file: $path", e)
        null
    }
}