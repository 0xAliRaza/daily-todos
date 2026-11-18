package com.aliraza.dailytasks

import android.app.Application

class DailyTasksApp : Application() {
    /** Every long-lived object the app needs, built once per process. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
