package com.aliraza.dailytodos.core.backup

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Brings an older backup up to the current version before it is decoded, so decoding stays strict and
 * there is one set of file classes. Each step changes only what its version changed.
 */
internal object BackupUpgrade {
    fun toCurrent(root: JsonObject): JsonObject {
        var file = root
        if (file.version() == 1) file = v1ToV2(file)
        return file
    }

    /** Version 2 records focus sessions and drops the "breaks" switch: every focus end offers a break. */
    private fun v1ToV2(file: JsonObject): JsonObject {
        val settings = file["settings"] as? JsonObject ?: return file
        return JsonObject(
            file + mapOf(
                "version" to JsonPrimitive(2),
                "settings" to JsonObject(settings - "breaksEnabled"),
                "focusSessions" to JsonArray(emptyList()),
            ),
        )
    }

    private fun JsonObject.version(): Int? = get("version")?.jsonPrimitive?.intOrNull
}
