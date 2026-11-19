package com.aliraza.dailytodos.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The room the keyboard took under the add bar, kept while a picker hides the keyboard. The bar then
 * stays still behind the picker instead of dropping to the bottom and jumping back up after it.
 */
@Stable
internal class KeyboardHold {
    /** The keyboard's height when the picker opened, in pixels. */
    var height by mutableIntStateOf(0)
        private set

    /** Whether the bar keeps [height] of room under it. */
    var holding by mutableStateOf(false)
        private set

    /**
     * A picker opened over a keyboard [imeBottom] pixels tall; with no keyboard there is nothing to keep.
     * A picker opened straight after another, before the keyboard is back, keeps the room already held.
     */
    fun pickerOpened(imeBottom: Int) {
        if (holding) return
        height = imeBottom
        holding = imeBottom > 0
    }

    /** Whether the keyboard is back as tall as before, so it can take over from the held room. */
    fun isBack(imeBottom: Int): Boolean = imeBottom >= height

    fun release() {
        holding = false
    }
}

/**
 * Holds the keyboard's room while [picking], and after it until the keyboard is back, or for
 * [KEYBOARD_RETURN_MS] if it doesn't come back.
 */
@Composable
internal fun rememberKeyboardHold(picking: Boolean): KeyboardHold {
    val hold = remember { KeyboardHold() }
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    LaunchedEffect(picking) {
        if (picking) {
            hold.pickerOpened(ime.getBottom(density))
        } else if (hold.holding) {
            withTimeoutOrNull(KEYBOARD_RETURN_MS) { snapshotFlow { ime.getBottom(density) }.first(hold::isBack) }
            hold.release()
        }
    }
    return hold
}

/** Long enough for the keyboard to come back after a picker closes; it slides in within about 300 ms. */
private const val KEYBOARD_RETURN_MS = 800L
