package com.sunilsstartingseven.app.data.backup

import kotlinx.serialization.json.*

class BackupCodec {
    fun encode(backup: SunilsStartingSevenBackup): String {
        val json = buildJsonObject {
            put("format", backup.format)
            put("backupVersion", backup.backupVersion)
            put("createdAt", backup.createdAt)
            put("appVersion", backup.appVersion)
            put("databaseSchemaVersion", backup.databaseSchemaVersion)
            if (backup.backupVersion >= 3) put("seasons", JsonArray(backup.seasons.map { season ->
                buildJsonObject { put("id", season.id); put("name", season.name); put("createdAt", season.createdAt) }
            }))
            put("players", JsonArray(backup.players.map { player ->
                buildJsonObject {
                    put("id", player.id)
                    put("firstName", player.firstName)
                    put("lastInitial", player.lastInitial)
                    put("isActive", player.isActive)
                    if (backup.backupVersion >= 3) put("carryFairnessAcrossSeasons", player.carryFairnessAcrossSeasons)
                    put("createdAt", player.createdAt)
                    if (player.photoPath == null) put("photoPath", JsonNull) else put("photoPath", player.photoPath)
                }
            }))
            put("games", JsonArray(backup.games.map { game ->
                buildJsonObject {
                    put("id", game.id)
                    if (backup.backupVersion >= 3) {
                        put("seasonId", game.seasonId?.let { JsonPrimitive(it) } ?: JsonNull)
                        put("fieldPositions", JsonArray(game.fieldPositions.map { JsonPrimitive(it) }))
                        put("positionOptions", JsonArray(game.positionOptions.map { JsonPrimitive(it) }))
                    }
                    put("roundNumber", game.roundNumber)
                    put("gameDate", game.gameDate)
                    put("status", game.status)
                    put("notes", game.notes)
                    put("createdAt", game.createdAt)
                    if (game.photoPath == null) put("photoPath", JsonNull) else put("photoPath", game.photoPath)
                }
            }))
            put("attendance", JsonArray(backup.attendance.map { row ->
                buildJsonObject {
                    put("gameId", row.gameId)
                    put("playerId", row.playerId)
                    put("status", row.status)
                }
            }))
            put("assignments", JsonArray(backup.assignments.map { row ->
                buildJsonObject {
                    put("gameId", row.gameId)
                    put("rotationIndex", row.rotationIndex)
                    put("playerId", row.playerId)
                    put("playingState", row.playingState)
                    if (row.position == null) put("position", JsonNull) else put("position", row.position)
                }
            }))
        }
        return Json { prettyPrint = true }.encodeToString(JsonObject.serializer(), json)
    }

    fun decode(text: String): SunilsStartingSevenBackup {
        val root = runCatching { Json.parseToJsonElement(text).jsonObject }
            .getOrElse { throw BackupException("This backup file could not be read as JSON.") }
        val version = root.int("backupVersion")
        return SunilsStartingSevenBackup(
            format = root.string("format"),
            backupVersion = root.int("backupVersion"),
            createdAt = root.string("createdAt"),
            appVersion = root.string("appVersion"),
            databaseSchemaVersion = root.int("databaseSchemaVersion"),
            players = root.array("players").map { item ->
                val row = item.jsonObject
                BackupPlayerDto(row.long("id"), row.string("firstName"), row.string("lastInitial"), row.boolean("isActive"), row.long("createdAt"), row.nullableString("photoPath"), if (version >= 3) row.boolean("carryFairnessAcrossSeasons") else false)
            },
            games = root.array("games").map { item ->
                val row = item.jsonObject
                BackupGameDto(row.long("id"), row.int("roundNumber"), row.string("gameDate"), row.string("status"), row.string("notes"), row.long("createdAt"), row.nullableString("photoPath"),
                    if (version >= 3) row["seasonId"]?.jsonPrimitive?.longOrNull else 1L,
                    if (version >= 3) row.array("fieldPositions").map { it.jsonPrimitive.content } else com.sunilsstartingseven.app.domain.model.PlayingPosition.entries.map { it.name },
                    if (version >= 3) row.array("positionOptions").map { it.jsonPrimitive.content } else com.sunilsstartingseven.app.domain.model.PlayingPosition.entries.map { it.name })
            },
            attendance = root.array("attendance").map { item ->
                val row = item.jsonObject
                BackupAttendanceDto(row.long("gameId"), row.long("playerId"), row.string("status"))
            },
            seasons = if (version >= 3) root.array("seasons").map { item ->
                val row = item.jsonObject
                BackupSeasonDto(row.long("id"), row.string("name"), row.long("createdAt"))
            } else if (root.array("games").isNotEmpty()) listOf(BackupSeasonDto(1, "Earlier games", 0)) else emptyList(),
            assignments = root.array("assignments").map { item ->
                val row = item.jsonObject
                BackupAssignmentDto(row.long("gameId"), row.int("rotationIndex"), row.long("playerId"), row.string("playingState"), row.nullableString("position"))
            }
        )
    }

    private fun JsonObject.string(name: String): String = this[name]?.jsonPrimitive?.contentOrNull ?: throw BackupException("Backup is missing $name.")
    private fun JsonObject.nullableString(name: String): String? = this[name]?.jsonPrimitive?.contentOrNull
    private fun JsonObject.long(name: String): Long = this[name]?.jsonPrimitive?.longOrNull ?: throw BackupException("Backup has an invalid $name.")
    private fun JsonObject.int(name: String): Int = this[name]?.jsonPrimitive?.intOrNull ?: throw BackupException("Backup has an invalid $name.")
    private fun JsonObject.boolean(name: String): Boolean = this[name]?.jsonPrimitive?.booleanOrNull ?: throw BackupException("Backup has an invalid $name.")
    private fun JsonObject.array(name: String): JsonArray = this[name]?.jsonArray ?: throw BackupException("Backup is missing $name.")
}
