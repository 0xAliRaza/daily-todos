package com.aliraza.dailytodos.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliraza.dailytodos.core.focus.FocusPresets
import com.aliraza.dailytodos.core.focus.FocusStats
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TimerSignal
import com.aliraza.dailytodos.data.SettingsRepository
import com.aliraza.dailytodos.data.TaskFocusTotal
import com.aliraza.dailytodos.data.TaskRepository
import com.aliraza.dailytodos.data.TimerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The focus timer of one task at a time, and the focus stats. The countdown on screen is only a display;
 * the scheduler ends the rounds and records them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FocusViewModel(
    private val timers: TimerRepository,
    private val tasks: TaskRepository,
    private val settingsRepo: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    val timer: StateFlow<FocusTimer> = timers.observe().stateIn(viewModelScope, SharingStarted.Eagerly, FocusTimer.Idle)

    val settings: StateFlow<Settings> = settingsRepo.observe().stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        Settings(),
    )

    /** The title of the task the timer is busy with, for the now-focusing bar; null while idle. */
    val activeTitle: StateFlow<String?> = timer.map { t -> t.taskId.takeIf { t.isActive } }
        .distinctUntilChanged()
        .flatMapLatest { id -> id?.let { tasks.observeTask(it).map { task -> task?.title } } ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val stats: StateFlow<FocusStats> = combine(timers.observeSessions(), tasks.observeTitles()) { sessions, titles ->
        FocusStats.from(sessions, titles, LocalDate.now(clock), clock.zone)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(STOP_MS),
        FocusStats.from(emptyList(), emptyMap(), LocalDate.now(clock), clock.zone),
    )

    fun now(): Instant = clock.instant()

    /** The device's zone, read each time, so the end times on screen follow a time-zone change. */
    val zone: ZoneId get() = clock.zone

    fun today(): LocalDate = LocalDate.now(clock)

    fun task(id: String): Flow<Task?> = tasks.observeTask(id)

    fun focused(taskId: String): Flow<TaskFocusTotal> = timers.observeTaskFocus(taskId)

    fun start(taskId: String) = launch { timers.start(taskId) }

    fun startBreak() = launch { timers.startBreak() }

    fun skipBreak() = launch { timers.skipBreak() }

    fun pause() = launch { timers.pause() }

    fun resume() = launch { timers.resume() }

    fun stop() = launch { timers.stop() }

    /** Picks the focus length; one that is not a preset also becomes the fourth chip. */
    fun setFocusMinutes(minutes: Int) = launch {
        settingsRepo.update { s ->
            val picked = minutes.coerceIn(Settings.FOCUS_MINUTES_RANGE)
            s.copy(
                focusMinutes = picked,
                focusCustomMinutes = FocusPresets.customAfter(FocusPresets.FOCUS, s.focusCustomMinutes, picked),
            )
        }
    }

    /** Picks the break length; one that is not a preset also becomes the fourth chip. */
    fun setBreakMinutes(minutes: Int) = launch {
        settingsRepo.update { s ->
            val picked = minutes.coerceIn(Settings.BREAK_MINUTES_RANGE)
            s.copy(
                breakMinutes = picked,
                breakCustomMinutes = FocusPresets.customAfter(FocusPresets.BREAK, s.breakCustomMinutes, picked),
            )
        }
    }

    fun setSignal(signal: TimerSignal) = launch { settingsRepo.update { it.copy(timerSignal = signal) } }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
