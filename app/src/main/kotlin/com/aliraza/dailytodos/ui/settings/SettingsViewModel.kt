package com.aliraza.dailytodos.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.backup.BackupContent
import com.aliraza.dailytodos.core.backup.BackupReadResult
import com.aliraza.dailytodos.core.model.NewTask
import com.aliraza.dailytodos.core.model.ReminderLogEntry
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.data.BackupRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.ReminderLogDao
import com.aliraza.dailytodos.data.db.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Settings, backups and the reminder health screen. */
class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val backup: BackupRepository,
    private val tasks: TaskRepository,
    reminderLog: ReminderLogDao,
    private val clock: Clock,
) : ViewModel() {
    val settings: StateFlow<Settings> = settingsRepo.observe().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings(),
    )

    /** The newest reminders first; the log keeps no titles. */
    val history: StateFlow<List<ReminderLogEntry>> = reminderLog.observeAll()
        .map { rows -> rows.take(HISTORY_SHOWN).map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    /** A checked backup waiting for the user to confirm the replace. */
    val pendingImport = MutableStateFlow<BackupContent?>(null)

    private val messages = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val message: SharedFlow<Int> = messages

    val zone: java.time.ZoneId get() = clock.zone

    fun backupFileName(): String = "daily-todos-backup-${java.time.LocalDate.now(clock)}.json"

    fun update(change: (Settings) -> Settings) {
        viewModelScope.launch { settingsRepo.update(change) }
    }

    /** Writes a backup through the stream the system file picker gave. */
    fun export(open: () -> OutputStream?) {
        viewModelScope.launch {
            val file = backup.export()
            val written = withContext(Dispatchers.IO) {
                try {
                    open()?.use { it.write(file.text.toByteArray(Charsets.UTF_8)) } != null
                } catch (_: IOException) {
                    false
                }
            }
            if (written) backup.markBackedUp(file.at)
            say(if (written) R.string.backup_saved else R.string.backup_failed)
        }
    }

    /** Reads and checks a picked file; nothing changes until [confirmImport]. */
    fun readImport(open: () -> InputStream?) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    open()?.use { backup.read(it) }
                } catch (_: IOException) {
                    null
                }
            }
            when (result) {
                is BackupReadResult.Accepted -> pendingImport.value = result.content
                else -> say(R.string.backup_rejected)
            }
        }
    }

    fun confirmImport() {
        val content = pendingImport.value ?: return
        pendingImport.value = null
        viewModelScope.launch {
            backup.restore(content)
            say(R.string.backup_restored)
        }
    }

    /**
     * A real task in the default list, due a minute from now plus the heads-up, so the test travels the
     * exact path every reminder takes.
     */
    fun sendTestReminder(title: String) {
        viewModelScope.launch {
            val s = settings.value
            val at = LocalDateTime.now(
                clock,
            ).truncatedTo(ChronoUnit.MINUTES).plusMinutes(TEST_MINUTES + s.headsUp.minutes)
            tasks.addTask(NewTask(title = title, dueDate = at.toLocalDate(), dueTime = at.toLocalTime()))
            say(R.string.health_test_sent)
        }
    }

    private fun say(@StringRes text: Int) {
        messages.tryEmit(text)
    }

    private companion object {
        const val HISTORY_SHOWN = 30
        const val STOP_MS = 5_000L
        const val TEST_MINUTES = 1L
    }
}
