package com.pawmino.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.KSerializer

/**
 * Central serialization configuration and tolerant decoding helpers.
 *
 * The [json] instance is deliberately forgiving so stored data survives app updates:
 *  - ignoreUnknownKeys: fields removed in a later version don't break old JSON
 *  - coerceInputValues + explicit defaults: unknown enum values fall back to a default
 *  - isLenient: minor formatting differences are tolerated
 */
object JsonStore {

    val json: Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        allowStructuredMapKeys = true
    }

    fun <T> encodeList(serializer: KSerializer<List<T>>, value: List<T>): String =
        json.encodeToString(serializer, value)

    /**
     * Decode a JSON array, recovering from a single malformed element instead of discarding
     * every valid one. If the whole string is unusable, returns an empty list.
     */
    fun <T> decodeListSafe(
        elementSerializer: KSerializer<T>,
        listSerializer: KSerializer<List<T>>,
        raw: String?
    ): List<T> {
        if (raw.isNullOrBlank()) return emptyList()
        // Fast path: the entire list parses cleanly.
        try {
            return json.decodeFromString(listSerializer, raw)
        } catch (e: Exception) {
            // Fall through to per-element recovery.
        }
        return try {
            val array = json.parseToJsonElement(raw) as? JsonArray ?: return emptyList()
            array.mapNotNull { element ->
                try {
                    json.decodeFromJsonElement(elementSerializer, element)
                } catch (e: Exception) {
                    null // skip one malformed object, keep the rest
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun <T> decodeObjectSafe(serializer: KSerializer<T>, raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return try {
            json.decodeFromString(serializer, raw)
        } catch (e: Exception) {
            default
        }
    }
}
