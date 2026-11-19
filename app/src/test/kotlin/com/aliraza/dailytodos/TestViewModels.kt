package com.aliraza.dailytodos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import org.robolectric.shadows.ShadowLooper
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The ViewModels a test makes, kept in a store as an activity keeps them. Their flows read the database
 * for as long as their scopes live, so a test [clear]s them before it closes the database: a read still
 * running when it closes fails whichever test runs next.
 */
class TestViewModels {
    private val store = ViewModelStore()
    private val scopes = mutableListOf<Job>()

    inline fun <reified T : ViewModel> add(noinline make: () -> T): T = add(T::class, make)

    fun <T : ViewModel> add(type: KClass<T>, make: () -> T): T {
        val factory = viewModelFactory { addInitializer(type) { make() } }
        val model = ViewModelProvider.create(store, factory)["model${scopes.size}", type]
        scopes += model.viewModelScope.coroutineContext.job
        return model
    }

    /**
     * Clears every ViewModel, as leaving its screen for good does, and returns once the work each one
     * started has finished, reads in flight included.
     */
    fun clear(timeout: Duration = 5.seconds) {
        store.clear()
        val deadline = System.nanoTime() + timeout.inWholeNanoseconds
        while (scopes.any { !it.isCompleted }) {
            check(System.nanoTime() < deadline) { "ViewModel work still running $timeout after clearing" }
            // Cancelled work finishes on the main thread, whose looper runs only when the test idles it.
            ShadowLooper.idleMainLooper()
            Thread.yield()
        }
    }
}
