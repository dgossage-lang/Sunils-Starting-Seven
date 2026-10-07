package com.sunilsstartingseven.app.domain.model

import kotlinx.serialization.json.*

/** Standard position names stay compatible with v1/v2 storage and backups. */
object PositionCodec {
    fun encode(position: PlayingPosition): String = if (!position.isCustom) position.name else buildJsonObject {
        put("id", position.name)
        put("label", position.shortLabel)
        put("name", position.fullLabel)
        put("family", position.family.name)
        put("x", position.x)
        put("y", position.y)
    }.toString()

    fun decode(value: String): PlayingPosition {
        PlayingPosition.entries.find { it.name == value }?.let { return it }
        val row = Json.parseToJsonElement(value).jsonObject
        val id = row.getValue("id").jsonPrimitive.content
        val label = row.getValue("label").jsonPrimitive.content
        val name = row.getValue("name").jsonPrimitive.content
        val x = row.getValue("x").jsonPrimitive.float
        val y = row.getValue("y").jsonPrimitive.float
        require(id.matches(Regex("CUSTOM_[A-Za-z0-9-]{1,64}"))) { "Invalid custom position ID." }
        require(label.isNotBlank() && label.length <= 5 && name.isNotBlank() && name.length <= 40) { "Enter a position name and a short label of up to five characters." }
        require(x.isFinite() && y.isFinite() && x in .12f.. .88f && y in .12f.. .88f) { "Position must stay within the pitch." }
        return PlayingPosition(id, PositionFamily.valueOf(row.getValue("family").jsonPrimitive.content), label, name, x, y)
    }

    fun encodeLayout(positions: List<PlayingPosition>): String = JsonArray(positions.map { JsonPrimitive(encode(it)) }).toString()
    fun decodeLayout(value: String): List<PlayingPosition> =
        if (value.startsWith("[")) Json.parseToJsonElement(value).jsonArray.map { decode(it.jsonPrimitive.content) }
        else value.split(',').map(::decode)
}
