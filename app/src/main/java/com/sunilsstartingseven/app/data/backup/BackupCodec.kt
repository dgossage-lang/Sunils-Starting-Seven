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
            put("players", JsonArray(backup.players.map { player ->
                buildJsonObject {
                    put("id", player.id)
                    put("firstName", player.firstName)
                    put("lastInitial", player.lastInitial)
                    put("isActive", player.isActive)
                    put("createdAt", player.createdAt)
                }
            }))
            put("games", JsonArray(backup.games.map { game ->
                buildJsonObject {
                    put("id", game.id)
                    put("roundNumber", game.roundNumber)
                    put("gameDate", game.gameDate)
                    put("status", game.status)
                    put("notes", game.notes)
                    put("createdAt", game.createdAt)
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
        return SunilsStartingSevenBackup(
            format = root.string("format"),
            backupVersion = root.int("backupVersion"),
            createdAt = root.string("createdAt"),
            appVersion = root.string("appVersion"),
            databaseSchemaVersion = root.int("databaseSchemaVersion"),
            players = root.array("players").map { item ->
                val row = item.jsonObject
                BackupPlayerDto(row.long("id"), row.string("firstName"), row.string("lastInitial"), row.boolean("isActive"), row.long("createdAt"))
            },
            games = root.array("games").map { item ->
                val row = item.jsonObject
                BackupGameDto(row.long("id"), row.int("roundNumber"), row.string("gameDate"), row.string("status"), row.string("notes"), row.long("createdAt"))
            },
            attendance = root.array("attendance").map { item ->
                val row = item.jsonObject
                BackupAttendanceDto(row.long("gameId"), row.long("playerId"), row.string("status"))
            },
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
