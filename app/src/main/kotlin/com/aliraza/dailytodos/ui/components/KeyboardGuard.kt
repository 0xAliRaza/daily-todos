package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Knows which text field in the content has the keyboard, so [KeyboardGuardBox] can tell a tap on
 * that field from a tap anywhere else.
 */
@Stable
class KeyboardGuard {
    /** Window bounds of the focused field; null while no guarded field has focus. */
    var field: Rect? by mutableStateOf(null)
        private set

    internal fun hold(bounds: Rect) {
        field = bounds
    }

    internal fun release(bounds: Rect?) {
        if (bounds == null || field == bounds) field = null
    }
}

val LocalKeyboardGuard = staticCompositionLocalOf { KeyboardGuard() }

/** One text field's tie to the screen's [KeyboardGuard]; attach it with [Modifier.guarded]. */
class GuardedField internal constructor(private val guard: KeyboardGuard) {
    private var bounds: Rect? = null
    private var focused = false

    internal fun positioned(now: Rect) {
        bounds = now
        if (focused) guard.hold(now)
    }

    internal fun focusChanged(isFocused: Boolean) {
        focused = isFocused
        val current = bounds
        if (focused && current != null) guard.hold(current) else guard.release(current)
    }

    internal fun dispose() = guard.release(bounds)
}

@Composable
fun rememberGuardedField(): GuardedField {
    val guard = LocalKeyboardGuard.current
    val field = remember(guard) { GuardedField(guard) }
    DisposableEffect(field) { onDispose { field.dispose() } }
    return field
}

/** Tells the guard where this field is while it has focus, so taps on it still reach it. */
fun Modifier.guarded(field: GuardedField): Modifier = this
    .onGloballyPositioned { field.positioned(it.boundsInWindow()) }
    .onFocusChanged { field.focusChanged(it.isFocused) }

/**
 * The screen's content. While a guarded field has focus, or while [alsoWhen] holds (the add bar,
 * which sits outside this box), the first tap outside the field only closes the keyboard: it goes no
 * further, so it can't open a task, tick a box or start a swipe by accident.
 */
@Composable
fun KeyboardGuardBox(alsoWhen: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val guard = LocalKeyboardGuard.current
    val focus = LocalFocusManager.current
    val always by rememberUpdatedState(alsoWhen)
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .onGloballyPositioned { origin = it.positionInWindow() }
            .pointerInput(guard) {
                awaitEachGesture {
                    // The initial pass reaches this box before anything inside it sees the touch.
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val field = guard.field
                    val onField = field?.contains(down.position + origin) == true
                    if (onField || (field == null && !always)) return@awaitEachGesture
                    down.consume()
                    focus.clearFocus()
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) { content() }
}
