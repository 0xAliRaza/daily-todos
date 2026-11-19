package com.aliraza.dailytodos.data

import androidx.room.withTransaction
import com.aliraza.dailytodos.core.backup.BackupCodec
import com.aliraza.dailytodos.core.backup.BackupContent
import com.aliraza.dailytodos.core.backup.BackupReadResult
import com.aliraza.dailytodos.core.backup.Limits
import com.aliraza.dailytodos.core.backup.RejectReason
import com.aliraza.dailytodos.data.db.AppDatabase
import com.aliraza.dailytodos.data.db.toDomain
import com.aliraza.dailytodos.data.db.toEntity
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.time.Clock
import java.time.Instant

/**
 * Export and import of everything as one JSON file (PLAN.md section 10, M8). The file pickers live in
 * the UI; this class turns the database into text and a checked file back into the database.
 */
class BackupRepository(
    private val db: AppDatabase,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val onScheduleChanged: suspend () -> Unit,
) {
    /** The text to write, stamped with its time. Call [markBackedUp] once it is safely written. */
    data class Export(val text: String, val at: Instant)

    suspend fun export(): Export {
        val at = clock.instant()
        val content = db.withTransaction { snapshot() }
        val stamped = content.copy(settings = content.settings.copy(lastBackupAt = at))
        return Export(BackupCodec.encode(stamped, at), at)
    }

    /** Records when the last backup was made, which Settings shows. */
    suspend fun markBackedUp(at: Instant) {
        settings.update { it.copy(lastBackupAt = at) }
    }

    /** Reads and checks a file without touching the database; the user confirms before [restore]. */
    fun read(input: InputStream): BackupReadResult {
        val bytes = readCapped(input) ?: return BackupReadResult.Rejected(RejectReason.TOO_LARGE, "file")
        val text = decodeUtf8(bytes) ?: return BackupReadResult.Rejected(RejectReason.MALFORMED, "encoding")
        return BackupCodec.decode(text)
    }

    /** Replaces everything with [content] in one transaction, then re-runs the reminder engine. */
    suspend fun restore(content: BackupContent) {
        db.withTransaction {
            db.subtasks().deleteAll()
            db.tasks().deleteAll()
            db.lists().deleteAll()
            db.reminderLog().deleteAll()
            db.focusSessions().deleteAll()
            db.lists().insertAll(content.lists.map { it.toEntity() })
            db.tasks().insertAll(content.tasks.map { it.toEntity() })
            db.subtasks().insertAll(content.subtasks.map { it.toEntity() })
            db.reminderLog().insertAll(content.reminderHistory.map { it.toEntity() })
            db.focusSessions().insertAll(content.focusSessions.map { it.toEntity() })
            db.settings().upsert(content.settings.toEntity())
        }
        onScheduleChanged()
    }

    /** Everything a backup holds, as it is now. */
    suspend fun snapshot(): BackupContent {
        val tasks = db.tasks().allKept().map { it.toDomain() }
        val taskIds = tasks.map { it.id }.toSet()
        return BackupContent(
            settings = settings.current(),
            lists = db.lists().all().map { it.toDomain() },
            tasks = tasks,
            subtasks = db.subtasks().allKept().map { it.toDomain() },
            reminderHistory = db.reminderLog().all().map { it.toDomain() },
            // Time on a task that is deleted, and so not in the file, still counts: it travels taskless.
            focusSessions = db.focusSessions().all().map { row ->
                row.toDomain().let { if (it.taskId in taskIds) it else it.copy(taskId = null) }
            },
        )
    }

    /** At most [Limits.MAX_FILE_BYTES]; null when the stream holds more. */
    private fun readCapped(input: InputStream): ByteArray? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > Limits.MAX_FILE_BYTES) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    /** Strict UTF-8: a file with invalid bytes is refused rather than read with replacement characters. */
    private fun decodeUtf8(bytes: ByteArray): String? = try {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (_: CharacterCodingException) {
        null
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
