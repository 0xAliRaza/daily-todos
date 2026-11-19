package com.aliraza.dailytodos.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * The transition between back stacks. A stack change animates to the new page; a back swipe seeks it
 * towards the page below, so that page peeks in under the finger, and the swipe then either goes back
 * or lets the page settle where it was. [backEnabled] false leaves back to someone else (a selection).
 *
 * Durations follow the system animator scale, which Compose applies to every animation, so turning
 * animations off in the phone's settings turns these off too.
 */
@Composable
internal fun rememberPageTransition(nav: Navigation, backEnabled: Boolean): Transition<List<String>> {
    val seekable = remember { SeekableTransitionState(nav.stack) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(nav.stack) { seekable.animateTo(nav.stack) }
    PredictiveBackHandler(enabled = backEnabled && nav.canGoBack) { swipe ->
        val below = nav.stack.dropLast(1)
        try {
            swipe.collect { seekable.seekTo(it.progress, targetState = below) }
            nav.back()
        } catch (e: CancellationException) {
            // The swipe was let go short of going back. This coroutine is already cancelled, so the
            // page settles back from a scope of its own.
            scope.launch { seekable.animateTo(seekable.currentState) }
            throw e
        }
    }
    return rememberTransition(seekable, label = "page")
}

/**
 * How one page gives way to the next: a fade between tabs; a push slides the new page in from the right
 * over the old one, which drifts a little left; a pop plays that in reverse.
 */
internal fun AnimatedContentTransitionScope<List<String>>.pageTransform(): ContentTransform =
    when (transitionKind(initialState, targetState)) {
        TransitionKind.TAB -> fadeIn() togetherWith fadeOut()

        TransitionKind.PUSH ->
            slideInHorizontally(tween(PAGE_MS, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(PAGE_MS)) togetherWith
                slideOutHorizontally(tween(PAGE_MS, easing = FastOutSlowInEasing)) { -it / DRIFT } +
                fadeOut(tween(PAGE_MS))

        TransitionKind.POP -> ContentTransform(
            targetContentEnter = slideInHorizontally(tween(PAGE_MS, easing = FastOutSlowInEasing)) { -it / DRIFT } +
                fadeIn(tween(PAGE_MS)),
            initialContentExit = slideOutHorizontally(tween(PAGE_MS, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(PAGE_MS)),
            // The page going away stays on top as it slides off, uncovering the one below.
            targetContentZIndex = -1f,
        )
    }

private const val PAGE_MS = 300

/** The old page moves a quarter of the width while the new one covers it. */
private const val DRIFT = 4
