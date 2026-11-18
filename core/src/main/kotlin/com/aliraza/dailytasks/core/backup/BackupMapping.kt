package com.aliraza.dailytasks.core.backup

import com.aliraza.dailytasks.core.model.FiredBy
import com.aliraza.dailytasks.core.model.HeadsUp
import com.aliraza.dailytasks.core.model.Priority
import com.aliraza.dailytasks.core.model.ReminderLogEntry
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.model.Subtask
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.core.model.TaskList
import com.aliraza.dailytasks.core.model.ThemeMode
import com.aliraza.dailytasks.core.model.TimerSignal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Domain to file. */
internal object ToFile {
    private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")

    fun settings(s: Settings) = BackupSettings(
        defaultReminderTime = s.defaultReminderTime.format(hourMinute),
        undatedGetsTomorrow = s.undatedGetsTomorrow,
        dateOnlyGetsReminder = s.dateOnlyGetsReminder,
        headsUpMinutes = s.headsUp.minutes,
        overdueReminders = s.overdueReminders,
        overdueReminderTime = s.overdueReminderTime.format(hourMinute),
        dndOverride = s.dndOverride,
        focusMinutes = s.focusMinutes,
        breakMinutes = s.breakMinutes,
        breaksEnabled = s.breaksEnabled,
        timerSignal = s.timerSignal.name,
        theme = s.theme.name,
        lastBackupAt = s.lastBackupAt?.toString(),
    )

    fun list(l: TaskList) = BackupList(l.id, l.name, l.position, l.isDefault)

    fun task(t: Task) = BackupTask(
        id = t.id,
        listId = t.listId,
        title = t.title,
        notes = t.notes,
        priority = t.priority.level,
        dueDate = t.dueDate?.toString(),
        dueTime = t.dueTime?.format(hourMinute),
        position = t.position,
        snoozedUntil = t.snoozedUntil?.toString(),
        notifiedFor = t.notifiedFor?.toString(),
        completedAt = t.completedAt?.toString(),
        createdAt = t.createdAt.toString(),
        updatedAt = t.updatedAt.toString(),
    )

    fun subtask(s: Subtask) = BackupSubtask(s.id, s.taskId, s.title, s.done, s.position)

    fun reminder(r: ReminderLogEntry) =
        BackupReminder(r.taskId, r.dueAt.toString(), r.firedAt.toString(), r.firedBy.name)
}

/** File to domain. Every value is parsed strictly; a bad one throws [InvalidBackup] naming the field. */
internal object FromFile {
    private val hourMinute = DateTimeFormatter.ofPattern("HH:mm")

    fun settings(s: BackupSettings) = Settings(
        defaultReminderTime = time(s.defaultReminderTime, "settings.defaultReminderTime"),
        undatedGetsTomorrow = s.undatedGetsTomorrow,
        dateOnlyGetsReminder = s.dateOnlyGetsReminder,
        headsUp = HeadsUp.entries.firstOrNull { it.minutes == s.headsUpMinutes }
            ?: throw InvalidBackup("settings.headsUpMinutes"),
        overdueReminders = s.overdueReminders,
        overdueReminderTime = time(s.overdueReminderTime, "settings.overdueReminderTime"),
        dndOverride = s.dndOverride,
        focusMinutes = s.focusMinutes.also { check(it in Settings.FOCUS_MINUTES_RANGE, "settings.focusMinutes") },
        breakMinutes = s.breakMinutes.also { check(it in Settings.BREAK_MINUTES_RANGE, "settings.breakMinutes") },
        breaksEnabled = s.breaksEnabled,
        timerSignal = enumOf<TimerSignal>(s.timerSignal, "settings.timerSignal"),
        theme = enumOf<ThemeMode>(s.theme, "settings.theme"),
        lastBackupAt = s.lastBackupAt?.let { instant(it, "settings.lastBackupAt") },
    )

    fun list(l: BackupList) = TaskList(l.id, l.name, l.position, l.isDefault)

    fun task(t: BackupTask): Task {
        val field = "tasks[${t.id}]"
        return Task(
            id = t.id,
            listId = t.listId,
            title = t.title,
            notes = t.notes,
            priority = Priority.entries.firstOrNull { it.level == t.priority }
                ?: throw InvalidBackup("$field.priority"),
            dueDate = t.dueDate?.let { date(it, "$field.dueDate") },
            dueTime = t.dueTime?.let { time(it, "$field.dueTime") },
            position = t.position,
            snoozedUntil = t.snoozedUntil?.let { instant(it, "$field.snoozedUntil") },
            notifiedFor = t.notifiedFor?.let { instant(it, "$field.notifiedFor") },
            completedAt = t.completedAt?.let { instant(it, "$field.completedAt") },
            createdAt = instant(t.createdAt, "$field.createdAt"),
            updatedAt = instant(t.updatedAt, "$field.updatedAt"),
        )
    }

    fun subtask(s: BackupSubtask) = Subtask(s.id, s.taskId, s.title, s.done, s.position)

    fun reminder(r: BackupReminder) = ReminderLogEntry(
        taskId = r.taskId,
        dueAt = instant(r.dueAt, "reminderHistory.dueAt"),
        firedAt = instant(r.firedAt, "reminderHistory.firedAt"),
        firedBy = enumOf<FiredBy>(r.firedBy, "reminderHistory.firedBy"),
    )

    private fun check(ok: Boolean, field: String) {
        if (!ok) throw InvalidBackup(field)
    }

    private inline fun <T> parse(field: String, block: () -> T): T = try {
        block()
    } catch (_: java.time.DateTimeException) {
        throw InvalidBackup(field)
    }

    private fun date(text: String, field: String): LocalDate = parse(field) { LocalDate.parse(text) }

    private fun time(text: String, field: String): LocalTime = parse(field) { LocalTime.parse(text, hourMinute) }

    private fun instant(text: String, field: String): Instant = parse(field) { Instant.parse(text) }

    private inline fun <reified E : Enum<E>> enumOf(name: String, field: String): E =
        enumValues<E>().firstOrNull { it.name == name } ?: throw InvalidBackup(field)
}

/** A value in the file is not one this app writes. [field] names it for the error message. */
class InvalidBackup(val field: String) : Exception("Invalid value at $field")
