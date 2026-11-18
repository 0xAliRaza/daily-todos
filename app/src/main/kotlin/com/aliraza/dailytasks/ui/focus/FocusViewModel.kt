package com.aliraza.dailytasks.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.data.SettingsRepository
import com.aliraza.dailytasks.data.TaskRepository
import com.aliraza.dailytasks.data.TimerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant

/** The focus screen. The countdown on screen is only a display; the scheduler ends the rounds. */
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

    /** The task a new timer would start from, picked from the task sheet. */
    private val picked = MutableStateFlow<String?>(null)

    /** The task of the running timer, or the picked one while idle. */
    val taskTitle: StateFlow<String?> = combine(timer, picked) { t, p -> t.taskId ?: p }
        .flatMapLatest { id -> if (id == null) flowOf(null) else tasks.observeTask(id).map { it?.title } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MS), null)

    fun now(): Instant = clock.instant()

    fun pickTask(id: String?) {
        picked.value = id
    }

    fun start() = launch { timers.start(picked.value) }

    fun next() = launch { timers.next() }

    fun skipBreak() = launch { timers.skipBreak() }

    fun pause() = launch { timers.pause() }

    fun resume() = launch { timers.resume() }

    fun stop() = launch {
        timers.stop()
        picked.value = null
    }

    fun setFocusMinutes(minutes: Int) = launch {
        settingsRepo.update { it.copy(focusMinutes = minutes.coerceIn(Settings.FOCUS_MINUTES_RANGE)) }
    }

    fun setBreakMinutes(minutes: Int) = launch {
        settingsRepo.update { it.copy(breakMinutes = minutes.coerceIn(Settings.BREAK_MINUTES_RANGE)) }
    }

    fun setBreaks(enabled: Boolean) = launch { settingsRepo.update { it.copy(breaksEnabled = enabled) } }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_MS = 5_000L
    }
}
