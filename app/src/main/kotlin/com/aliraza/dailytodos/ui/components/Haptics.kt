package com.aliraza.dailytodos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * The one way the app gives haptic feedback: a short tick that does nothing when haptics are off in
 * settings. Used for completing a task, the timer's steppers, presets and start, and swipe thresholds.
 */
@Composable
fun rememberTick(type: HapticFeedbackType = HapticFeedbackType.SegmentTick): () -> Unit {
    val feedback = LocalHapticFeedback.current
    val on = LocalHapticsOn.current
    return remember(feedback, on, type) { if (on) ({ feedback.performHapticFeedback(type) }) else ({}) }
}
