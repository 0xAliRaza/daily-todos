package com.aliraza.dailytasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aliraza.dailytasks.ui.theme.DailyTasksTheme
import com.aliraza.dailytasks.ui.today.TodayScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DailyTasksTheme {
                TodayScreen()
            }
        }
    }
}
