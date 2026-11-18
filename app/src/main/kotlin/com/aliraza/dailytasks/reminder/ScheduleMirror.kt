package com.aliraza.dailytasks.reminder

import androidx.core.util.AtomicFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * A small copy of the upcoming reminders in device-protected storage, readable before the first unlock
 * after a restart, when the database is not (PLAN.md section 5, "Restart before unlock").
 *
 * Holds titles of upcoming reminders only; that trade-off is stated in SECURITY.md.
 */
class ScheduleMirror(file: File) {

    @Serializable
    data class Entry(val taskId: String, val title: String, val atMillis: Long, val overdue: Boolean = false)

    @Serializable
    data class Fired(val taskId: String, val atMillis: Long, val firedAtMillis: Long)

    /**
     * @property upcoming the next reminders, earliest first.
     * @property firedBeforeUnlock reminders posted from this file while locked, to be recorded in the
     *   database after unlock.
     * @property dndOverride whether reminders use the channel that passes Do Not Disturb.
     */
    @Serializable
    data class Snapshot(
        val version: Int = VERSION,
        val upcoming: List<Entry> = emptyList(),
        val firedBeforeUnlock: List<Fired> = emptyList(),
        val dndOverride: Boolean = false,
    )

    private val atomic = AtomicFile(file)
    private val json = Json { ignoreUnknownKeys = true }

    /** The stored snapshot, or an empty one when there is none or it cannot be read. */
    fun read(): Snapshot = try {
        json.decodeFromString(Snapshot.serializer(), atomic.readFully().decodeToString())
            .takeIf { it.version == VERSION }
            ?: Snapshot()
    } catch (_: IOException) {
        Snapshot()
    } catch (_: SerializationException) {
        Snapshot()
    } catch (_: IllegalArgumentException) {
        Snapshot()
    }

    fun write(snapshot: Snapshot) {
        val stream = atomic.startWrite()
        try {
            stream.write(json.encodeToString(Snapshot.serializer(), snapshot).encodeToByteArray())
            atomic.finishWrite(stream)
        } catch (e: IOException) {
            atomic.failWrite(stream)
            throw e
        }
    }

    companion object {
        const val FILE_NAME = "schedule-mirror.json"
        private const val VERSION = 1

        /** How many upcoming reminders are copied; enough for days of normal use. */
        const val MAX_ENTRIES = 50
    }
}
