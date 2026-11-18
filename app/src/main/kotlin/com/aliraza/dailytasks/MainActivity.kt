package com.aliraza.dailytasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aliraza.dailytasks.ui.theme.DailyTasksTheme
import com.aliraza.dailytasks.ui.today.TodayScreen
import com.aliraza.dailytasks.ui.today.TodayViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as DailyTasksApp).container
        val factory = viewModelFactory {
            initializer { TodayViewModel(container.tasks, container.lists, container.clock) }
        }
        setContent {
            DailyTasksTheme {
                TodayScreen(viewModel(factory = factory))
            }
        }
    }

    companion object {
        /** Set when the activity is opened from a reminder. */
        const val EXTRA_TASK_ID = "task_id"
    }
}
