package com.aliraza.dailytasks.core.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

/** Why an import was refused. The app turns each into a plain-language message. */
enum class RejectReason { NOT_A_BACKUP, NEWER_VERSION, MALFORMED, TOO_LARGE, INVALID }

sealed interface BackupReadResult {
    data class Accepted(val content: BackupContent) : BackupReadResult

    data class Rejected(val reason: RejectReason, val detail: String) : BackupReadResult
}

/**
 * Writes and reads backup files. Reading treats the file as hostile (PLAN.md section 10, M8): the format
 * and version are checked first, unknown fields are refused, every count and length is capped, ids must
 * be unique and references must resolve, and every value must parse.
 */
object BackupCodec {
    const val FORMAT = "daily-tasks-backup"
    const val VERSION = 2

    /** The oldest version still read; [BackupUpgrade] brings it up to [VERSION]. */
    const val OLDEST_VERSION = 1

    private val writer = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    private val reader = Json { ignoreUnknownKeys = false }

    fun encode(content: BackupContent, exportedAt: Instant): String = writer.encodeToString(
        BackupFile.serializer(),
        BackupFile(
            format = FORMAT,
            version = VERSION,
            exportedAt = exportedAt.toString(),
            settings = ToFile.settings(content.settings),
            lists = content.lists.map(ToFile::list),
            tasks = content.tasks.map(ToFile::task),
            subtasks = content.subtasks.map(ToFile::subtask),
            reminderHistory = content.reminderHistory.map(ToFile::reminder),
            focusSessions = content.focusSessions.map(ToFile::session),
        ),
    )

    fun decode(text: String): BackupReadResult = when (val parsed = parse(text)) {
        is Parsed.Failed -> parsed.rejection
        is Parsed.Decoded -> BackupValidator.problem(parsed.file) ?: toContent(parsed.file)
    }

    private sealed interface Parsed {
        data class Decoded(val file: BackupFile) : Parsed

        data class Failed(val rejection: BackupReadResult.Rejected) : Parsed
    }

    private fun parse(text: String): Parsed {
        // The app caps the bytes it reads; this guards callers that hand over text directly.
        if (text.length > Limits.MAX_FILE_BYTES) return Parsed.Failed(reject(RejectReason.TOO_LARGE, "file"))
        return try {
            when (val root = reader.parseToJsonElement(text) as? JsonObject) {
                null -> Parsed.Failed(reject(RejectReason.NOT_A_BACKUP, "format"))

                else -> checkHeader(root)?.let(Parsed::Failed)
                    ?: Parsed.Decoded(
                        reader.decodeFromJsonElement(BackupFile.serializer(), BackupUpgrade.toCurrent(root)),
                    )
            }
        } catch (e: SerializationException) {
            Parsed.Failed(reject(RejectReason.MALFORMED, e.message.orEmpty()))
        } catch (e: IllegalArgumentException) {
            Parsed.Failed(reject(RejectReason.MALFORMED, e.message.orEmpty()))
        }
    }

    private fun toContent(file: BackupFile): BackupReadResult = try {
        BackupReadResult.Accepted(
            BackupContent(
                settings = FromFile.settings(file.settings),
                lists = file.lists.map(FromFile::list),
                tasks = file.tasks.map(FromFile::task),
                subtasks = file.subtasks.map(FromFile::subtask),
                reminderHistory = file.reminderHistory.map(FromFile::reminder),
                focusSessions = file.focusSessions.map(FromFile::session),
            ),
        )
    } catch (e: InvalidBackup) {
        reject(RejectReason.INVALID, e.field)
    }

    /** Returns a rejection when this is not our file or comes from a newer app version. */
    private fun checkHeader(root: JsonObject): BackupReadResult.Rejected? {
        val format = root["format"]?.jsonPrimitive?.contentOrNull
        val version = root["version"]?.jsonPrimitive?.intOrNull
        return when {
            format != FORMAT -> reject(RejectReason.NOT_A_BACKUP, "format")
            version == null -> reject(RejectReason.MALFORMED, "version")
            version > VERSION -> reject(RejectReason.NEWER_VERSION, "version $version")
            version < OLDEST_VERSION -> reject(RejectReason.INVALID, "version $version")
            else -> null
        }
    }

    internal fun reject(reason: RejectReason, detail: String) = BackupReadResult.Rejected(reason, detail)
}
