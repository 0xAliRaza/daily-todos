package com.aliraza.dailytodos

import android.app.Application
import androidx.core.os.UserManagerCompat
import com.aliraza.dailytodos.core.model.FiredBy
import kotlinx.coroutines.launch

class DailyTodosApp : Application() {
    /** Every long-lived object the app needs, built once per process. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Every process start re-arms the alarm and catches up what was missed. Before the first unlock
        // the database is unreadable; the boot receiver handles that case from the mirror.
        if (UserManagerCompat.isUserUnlocked(this)) {
            container.appScope.launch {
                container.tasks.purgeDeleted()
                container.scheduler.run(FiredBy.CATCH_UP)
            }
        }
    }
}
