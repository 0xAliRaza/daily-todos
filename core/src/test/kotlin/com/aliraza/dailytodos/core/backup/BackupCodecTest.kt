package com.aliraza.dailytodos.core.backup

import com.aliraza.dailytodos.core.focus.FocusSession
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.FiredBy
import com.aliraza.dailytodos.core.model.HeadsUp
import com.aliraza.dailytodos.core.model.Priority
import com.aliraza.dailytodos.core.model.ReminderLogEntry
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.Subtask
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.core.model.ThemeMode
import com.aliraza.dailytodos.core.model.TimerSignal
import com.aliraza.dailytodos.core.model.WeekStart
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class BackupCodecTest {
    private val exportedAt = Instant.parse("2026-11-18T12:00:00Z")
    private val json = Json { prettyPrint = true }

    private val sample = BackupContent(
        settings = Settings(
            defaultReminderTime = LocalTime.of(8, 30),
            undatedGetsTomorrow = false,
            headsUp = HeadsUp.THIRTY,
            overdueReminderTime = LocalTime.of(7, 0),
            dndOverride = true,
            focusMinutes = 50,
            breakMinutes = 10,
            focusCustomMinutes = 40,
            breakCustomMinutes = 7,
            timerSignal = TimerSignal.VIBRATE,
            theme = ThemeMode.DARK,
            clockFormat = ClockFormat.H24,
            weekStart = WeekStart.SUNDAY,
            haptics = false,
            lastBackupAt = Instant.parse("2026-11-01T08:00:00Z"),
        ),
        lists = listOf(TaskList("default", "Daily", 0, true), TaskList("g", "Groceries", 1, false)),
        tasks = listOf(
            Task(
                id = "t1",
                listId = "default",
                title = "Call bank \"about\" the loan — ünïcødé ✓",
                notes = "line one\nline two",
                priority = Priority.P1,
                dueDate = LocalDate.of(2026, 11, 19),
                dueTime = LocalTime.of(16, 0),
                position = 3,
                snoozedUntil = Instant.parse("2026-11-19T15:10:00Z"),
                notifiedFor = Instant.parse("2026-11-19T15:00:00Z"),
                completedAt = Instant.parse("2026-11-19T16:30:00.123Z"),
                createdAt = Instant.parse("2026-11-18T09:00:00Z"),
                updatedAt = Instant.parse("2026-11-19T16:30:00.123Z"),
            ),
            Task(
                id = "t2",
                listId = "g",
                title = "Milk",
                createdAt = Instant.parse("2026-11-18T09:01:00Z"),
                updatedAt = Instant.parse("2026-11-18T09:01:00Z"),
            ),
        ),
        subtasks = listOf(Subtask("s1", "t1", "Find account number", true, 0)),
        reminderHistory = listOf(
            ReminderLogEntry(
                "t1",
                Instant.parse("2026-11-19T15:00:00Z"),
                Instant.parse("2026-11-19T15:00:02Z"),
                FiredBy.BEFORE_UNLOCK,
            ),
        ),
        focusSessions = listOf(
            FocusSession(
                "t1",
                Instant.parse("2026-11-19T09:00:00Z"),
                Instant.parse("2026-11-19T09:30:00Z"),
                Duration.ofMinutes(25),
            ),
            // Time on a task deleted since: no task, still counted.
            FocusSession(
                null,
                Instant.parse("2026-11-18T09:00:00Z"),
                Instant.parse("2026-11-18T09:12:30Z"),
                Duration.ofSeconds(750),
            ),
        ),
    )

    private fun valid(): String = BackupCodec.encode(sample, exportedAt)

    /** The sample file with [change] applied to its JSON tree. */
    private fun edited(change: (JsonObject) -> JsonObject): String =
        json.encodeToString(JsonObject.serializer(), change(json.parseToJsonElement(valid()).jsonObject))

    private fun file(change: (BackupFile) -> BackupFile): String {
        val original = json.decodeFromString(BackupFile.serializer(), valid())
        return json.encodeToString(BackupFile.serializer(), change(original))
    }

    private fun assertRejected(reason: RejectReason, text: String) {
        val result = BackupCodec.decode(text)
        assertEquals(result.toString(), reason, (result as? BackupReadResult.Rejected)?.reason)
    }

    @Test
    fun everythingSurvivesARoundTrip() {
        assertEquals(BackupReadResult.Accepted(sample), BackupCodec.decode(valid()))
    }

    @Test
    fun fileIsReadableJsonWithItsFormatAndVersion() {
        val root = json.parseToJsonElement(valid()).jsonObject
        assertEquals(JsonPrimitive("daily-todos-backup"), root["format"])
        assertEquals(JsonPrimitive(2), root["version"])
        assertTrue(valid().contains("\"dueTime\": \"16:00\""))
        assertTrue(valid().contains("\"dueDate\": \"2026-11-19\""))
    }

    @Test
    fun aBackupFromBeforeTheRenameStillImports() {
        val legacy = edited { JsonObject(it + ("format" to JsonPrimitive("daily-tasks-backup"))) }
        assertEquals(BackupReadResult.Accepted(sample), BackupCodec.decode(legacy))
    }

    @Test
    fun refusesWhatIsNotOurFile() {
        assertRejected(RejectReason.MALFORMED, "not json at all")
        assertRejected(RejectReason.NOT_A_BACKUP, "[1, 2, 3]")
        assertRejected(RejectReason.NOT_A_BACKUP, """{"hello": "world"}""")
        assertRejected(RejectReason.NOT_A_BACKUP, edited { JsonObject(it + ("format" to JsonPrimitive("other"))) })
    }

    /** A version 1 file, as 0.1 builds before focus sessions wrote it: a "breaks" switch, no sessions. */
    private fun versionOne(): String = edited { root ->
        val settings = root.getValue("settings").jsonObject
        JsonObject(
            root - "focusSessions" + mapOf(
                "version" to JsonPrimitive(1),
                "settings" to JsonObject(settings + ("breaksEnabled" to JsonPrimitive(false))),
            ),
        )
    }

    @Test
    fun aVersionOneFileStillImports() {
        assertEquals(
            BackupReadResult.Accepted(sample.copy(focusSessions = emptyList())),
            BackupCodec.decode(versionOne()),
        )
    }

    /** A version 2 file written before the fourth timer chip: no custom lengths in its settings. */
    @Test
    fun aVersionTwoFileWithoutCustomLengthsImportsWithTheDefaults() {
        val older = edited { root ->
            val settings = root.getValue("settings").jsonObject
            JsonObject(root + ("settings" to JsonObject(settings - "focusCustomMinutes" - "breakCustomMinutes")))
        }
        val expected = sample.settings.copy(
            focusCustomMinutes = Settings.DEFAULT_FOCUS_CUSTOM_MINUTES,
            breakCustomMinutes = Settings.DEFAULT_BREAK_CUSTOM_MINUTES,
        )
        assertEquals(BackupReadResult.Accepted(sample.copy(settings = expected)), BackupCodec.decode(older))
    }

    @Test
    fun exportsTheCustomLengths() {
        assertTrue(valid().contains("\"focusCustomMinutes\": 40"))
        assertTrue(valid().contains("\"breakCustomMinutes\": 7"))
    }

    /** A version 2 file written before the display settings: no clock, week start or haptics. */
    @Test
    fun aVersionTwoFileWithoutDisplaySettingsImportsWithTheDefaults() {
        val older = edited { root ->
            val settings = root.getValue("settings").jsonObject
            JsonObject(root + ("settings" to JsonObject(settings - "clockFormat" - "weekStart" - "haptics")))
        }
        val defaults = Settings()
        val expected = sample.settings.copy(
            clockFormat = defaults.clockFormat,
            weekStart = defaults.weekStart,
            haptics = defaults.haptics,
        )
        assertEquals(BackupReadResult.Accepted(sample.copy(settings = expected)), BackupCodec.decode(older))
    }

    @Test
    fun exportsTheDisplaySettings() {
        assertTrue(valid().contains("\"clockFormat\": \"H24\""))
        assertTrue(valid().contains("\"weekStart\": \"SUNDAY\""))
        assertTrue(valid().contains("\"haptics\": false"))
    }

    /** docs/backup-format.md documents every settings field the app writes. */
    @Test
    fun theFormatDocListsEverySetting() {
        val doc = File("../docs/backup-format.md").readText()
        val fields = json.parseToJsonElement(valid()).jsonObject.getValue("settings").jsonObject.keys
        fields.forEach { assertTrue("$it is not in the format doc", doc.contains("\"$it\":")) }
    }

    @Test
    fun refusesOtherVersions() {
        assertRejected(RejectReason.NEWER_VERSION, edited { JsonObject(it + ("version" to JsonPrimitive(3))) })
        assertRejected(RejectReason.INVALID, edited { JsonObject(it + ("version" to JsonPrimitive(0))) })
        assertRejected(RejectReason.MALFORMED, edited { JsonObject(it - "version") })
    }

    @Test
    fun refusesUnknownAndMissingFields() {
        assertRejected(RejectReason.MALFORMED, edited { JsonObject(it + ("extra" to JsonPrimitive(true))) })
        assertRejected(RejectReason.MALFORMED, edited { JsonObject(it - "tasks") })
        assertRejected(RejectReason.MALFORMED, edited { JsonObject(it - "focusSessions") })
    }

    @Test
    fun refusesTooManyItems() {
        val many = List(Limits.MAX_LISTS + 1) { BackupList("l$it", "List $it", it.toLong(), it == 0) }
        assertRejected(RejectReason.TOO_LARGE, file { it.copy(lists = many) })
    }

    @Test
    fun refusesOverlongAndBlankText() {
        val long = "x".repeat(Limits.MAX_TITLE_LENGTH + 1)
        assertRejected(RejectReason.INVALID, file { f -> f.copy(tasks = f.tasks.map { it.copy(title = long) }) })
        assertRejected(RejectReason.INVALID, file { f -> f.copy(tasks = f.tasks.map { it.copy(id = "") }) })
        assertRejected(RejectReason.INVALID, file { f -> f.copy(lists = f.lists.map { it.copy(name = " ") }) })
    }

    @Test
    fun refusesDuplicateIdsAndBrokenReferences() {
        assertRejected(RejectReason.INVALID, file { f -> f.copy(tasks = f.tasks.map { it.copy(id = "same") }) })
        assertRejected(RejectReason.INVALID, file { f -> f.copy(tasks = f.tasks.map { it.copy(listId = "gone") }) })
        assertRejected(
            RejectReason.INVALID,
            file { f -> f.copy(subtasks = f.subtasks.map { it.copy(taskId = "gone") }) },
        )
        assertRejected(
            RejectReason.INVALID,
            file { f -> f.copy(focusSessions = f.focusSessions.map { it.copy(taskId = "gone") }) },
        )
    }

    @Test
    fun needsExactlyOneDefaultList() {
        assertRejected(RejectReason.INVALID, file { f -> f.copy(lists = f.lists.map { it.copy(isDefault = false) }) })
        assertRejected(RejectReason.INVALID, file { f -> f.copy(lists = f.lists.map { it.copy(isDefault = true) }) })
    }

    @Test
    fun refusesValuesThisAppNeverWrites() {
        val cases = listOf<(BackupFile) -> BackupFile>(
            { f -> f.copy(tasks = f.tasks.map { it.copy(dueDate = "2026-02-30") }) },
            { f -> f.copy(tasks = f.tasks.map { it.copy(dueTime = "25:00") }) },
            { f -> f.copy(tasks = f.tasks.map { it.copy(priority = 7) }) },
            { f -> f.copy(tasks = f.tasks.map { it.copy(createdAt = "yesterday") }) },
            { f -> f.copy(settings = f.settings.copy(headsUpMinutes = 15)) },
            { f -> f.copy(settings = f.settings.copy(focusMinutes = 0)) },
            { f -> f.copy(settings = f.settings.copy(focusCustomMinutes = 181)) },
            { f -> f.copy(settings = f.settings.copy(breakCustomMinutes = 0)) },
            { f -> f.copy(settings = f.settings.copy(theme = "NEON")) },
            { f -> f.copy(settings = f.settings.copy(clockFormat = "H36")) },
            { f -> f.copy(settings = f.settings.copy(weekStart = "FRIDAY")) },
            { f -> f.copy(reminderHistory = f.reminderHistory.map { it.copy(firedBy = "MAGIC") }) },
            { f -> f.copy(focusSessions = f.focusSessions.map { it.copy(focusedMs = -1) }) },
            // More focus than the session lasted.
            { f -> f.copy(focusSessions = f.focusSessions.map { it.copy(focusedMs = 86_400_000) }) },
            { f -> f.copy(focusSessions = f.focusSessions.map { it.copy(endedAt = "2026-01-01T00:00:00Z") }) },
        )
        cases.forEach { assertRejected(RejectReason.INVALID, file(it)) }
    }
}
