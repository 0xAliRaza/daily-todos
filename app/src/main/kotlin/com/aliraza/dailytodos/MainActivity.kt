package com.aliraza.dailytodos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aliraza.dailytodos.core.focus.keepsScreenOn
import com.aliraza.dailytodos.core.model.ThemeMode
import com.aliraza.dailytodos.ui.AppRoot
import com.aliraza.dailytodos.ui.RequestNotificationPermission
import com.aliraza.dailytodos.ui.components.ProvideDisplaySettings
import com.aliraza.dailytodos.ui.focus.FocusViewModel
import com.aliraza.dailytodos.ui.focus.KeepScreenOn
import com.aliraza.dailytodos.ui.settings.SettingsViewModel
import com.aliraza.dailytodos.ui.tasks.TasksViewModel
import com.aliraza.dailytodos.ui.theme.DailyTodosTheme

class MainActivity : ComponentActivity() {
    /** What a notification asked to open; read by the UI, then cleared. */
    private var opened by mutableStateOf<Opened?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) opened = Opened.from(intent)
        val container = (application as DailyTodosApp).container
        val factory = viewModelFactory {
            initializer {
                TasksViewModel(
                    container.tasks,
                    container.subtasks,
                    container.lists,
                    container.settings,
                    container.clock,
                )
            }
            initializer {
                SettingsViewModel(
                    container.settings,
                    container.backup,
                    container.tasks,
                    container.lists,
                    container.database.reminderLog(),
                    container.clock,
                )
            }
            initializer { FocusViewModel(container.timers, container.tasks, container.settings, container.clock) }
        }
        setContent {
            val tasks: TasksViewModel = viewModel(factory = factory)
            val settings by tasks.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val focus: FocusViewModel = viewModel(factory = factory)
            val timer by focus.timer.collectAsStateWithLifecycle()
            KeepScreenOn(keepsScreenOn(settings.keepScreenOn, timer))
            DailyTodosTheme(dark = dark) {
                ProvideDisplaySettings(settings) {
                    RequestNotificationPermission()
                    AppRoot(
                        tasks = tasks,
                        settings = viewModel(factory = factory),
                        focus = focus,
                        opened = opened,
                        onOpenedHandled = { opened = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        opened = Opened.from(intent)
    }

    /** A task to show, or a task's focus timer. */
    sealed interface Opened {
        data class Task(val id: String) : Opened

        data class Focus(val taskId: String) : Opened

        companion object {
            fun from(intent: Intent?): Opened? {
                if (intent == null) return null
                return intent.getStringExtra(EXTRA_OPEN_FOCUS)?.let(::Focus)
                    ?: intent.getStringExtra(EXTRA_TASK_ID)?.let(::Task)
            }
        }
    }

    companion object {
        /** Set when the activity is opened from a reminder. */
        const val EXTRA_TASK_ID = "task_id"

        /** Set when the activity is opened from the focus timer's notification: the timer's task. */
        const val EXTRA_OPEN_FOCUS = "open_focus"
    }
}
