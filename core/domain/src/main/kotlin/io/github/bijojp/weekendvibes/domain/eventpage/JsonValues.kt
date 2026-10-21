package io.github.bijojp.weekendvibes.domain.eventpage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.jsoup.parser.Parser

// Small readers for the loosely shaped JSON event pages embed.

internal val LenientJson = Json { isLenient = true }

/** Parsed JSON, or null when it isn't valid. */
internal fun parseJsonOrNull(text: String): JsonElement? = runCatching { LenientJson.parseToJsonElement(text) }.getOrNull()

/** The text of a string value with HTML entities decoded and whitespace tidied, or null if blank or not a string. */
internal fun JsonElement?.text(): String? {
    val primitive = this as? JsonPrimitive ?: return null
    if (primitive is JsonNull || !primitive.isString) return null
    return Parser.unescapeEntities(primitive.content, false).trim().replace(Regex("\\s+"), " ").takeIf { it.isNotEmpty() }
}

internal fun JsonObject.text(key: String): String? = this[key].text()

internal fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.longOrNull

/** The schema.org types of an object; `@type` may be one name or a list. */
internal val JsonObject.types: List<String>
    get() = when (val type = this["@type"]) {
        is JsonArray -> type.mapNotNull { it.text() }
        else -> listOfNotNull(type.text())
    }
