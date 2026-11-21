package com.aliraza.dailytodos.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.backup.BackupContent
import com.aliraza.dailytodos.core.backup.BackupReadResult
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.TaskList
import com.aliraza.dailytodos.data.BackupRepository
import com.aliraza.dailytodos.data.ListRepository
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.db.ReminderLogDao
import com.aliraza.dailytodos.data.db.toDomain
import com.aliraza.dailytodos.reminder.TestReminder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock

/** Settings, backups and the reminder health screen. */
class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val backup: BackupRepository,
    private val tasks: TaskRepository,
    lists: ListRepository,
    reminderLog: ReminderLogDao,
    private val clock: Clock,
    private val testReminder: TestReminder,
) : ViewModel() {
    val settings: StateFlow<Settings> = settingsRepo.observe().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings(),
    )

    /** The default list's name as it is now, for the row that names it. */
    val defaultListName: StateFlow<String> = lists.observe()
        .map { all -> all.firstOrNull { it.isDefault }?.name ?: TaskList.DEFAULT_NAME }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), TaskList.DEFAULT_NAME)

    /**
     * The newest reminders first, each with its task's title looked up live by id: the log itself keeps
     * no titles, so a renamed task shows its new name and a deleted one shows as deleted.
     */
    val history: StateFlow<List<HistoryItem>> =
        combine(reminderLog.observeAll(), tasks.observeTitles()) { rows, titles ->
            historyItems(rows.take(HISTORY_SHOWN).map { it.toDomain() }, titles)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), emptyList())

    /** A checked backup waiting for the user to confirm the replace. */
    val pendingImport = MutableStateFlow<BackupContent?>(null)

    private val messages = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    /** Short messages to show, as string resources; one may name the app as its only argument. */
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
     * A test reminder a minute from now. It waits on the same alarm as every task's reminder and adds no
     * task; see [TestReminder].
     */
    fun sendTestReminder() {
        viewModelScope.launch {
            testReminder.send()
            say(R.string.health_test_sent)
        }
    }

    private fun say(@StringRes text: Int) {
        messages.tryEmit(text)
    }

    private companion object {
        const val HISTORY_SHOWN = 30
        const val STOP_MS = 5_000L
    }
}
