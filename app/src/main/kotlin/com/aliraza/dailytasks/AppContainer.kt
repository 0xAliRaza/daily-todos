package com.aliraza.dailytasks

import android.content.Context

/**
 * Manual dependency injection: the single place where the app's long-lived objects are created.
 * Grows as storage and the reminder engine are added.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
}
