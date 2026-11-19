package com.aliraza.dailytasks.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusPresets
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.model.Task
import com.aliraza.dailytasks.core.model.TimerSignal
import com.aliraza.dailytasks.data.TaskFocusTotal
import com.aliraza.dailytasks.ui.components.AppIcons
import com.aliraza.dailytasks.ui.components.RoundCheck
import com.aliraza.dailytasks.ui.components.timeLabel
import com.aliraza.dailytasks.ui.theme.tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin

private const val SECOND_MS = 1_000L
private const val FOCUS_STEP = 5
private const val BREAK_STEP = 1

/**
 * One task's focus timer: one screen in four moods (see [Mood]). The task sits at the top, the dial in the
 * middle and the controls at the bottom, in thumb reach; on a short screen they stack and scroll. Leaving
 * the screen never stops the timer. [onComplete] ticks the task off, which also stops its timer. [onGone]
 * leaves the screen once the task no longer exists.
 */
@Composable
fun FocusScreen(
    viewModel: FocusViewModel,
    taskId: String,
    padding: PaddingValues,
    onComplete: () -> Unit,
    onGone: () -> Unit,
) {
    val task by remember(taskId) { viewModel.task(taskId) }.collectAsStateWithLifecycle(null)
    val focused by remember(taskId) { viewModel.focused(taskId) }.collectAsStateWithLifecycle(TaskFocusTotal.None)
    val timer by viewModel.timer.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val activeTitle by viewModel.activeTitle.collectAsStateWithLifecycle()
    val current = task
    LaunchedEffect(current?.deletedAt) { if (current?.deletedAt != null) onGone() }
    if (current == null) return
    val now by rememberNow(viewModel, timer)
    val mood = moodOf(timer, taskId)
    val length = lengthPicker(mood, settings, viewModel)
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            TaskHeader(current, focused, onComplete)
            DialBlock(mood, timer, dialReading(mood, timer, settings, now), now, viewModel, length)
            Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)) {
                length?.let {
                    Presets(it.presets, it.minutes, it.onChange)
                    Spacer(Modifier.height(12.dp))
                }
                when (mood) {
                    Mood.READY -> ReadyControls(
                        settings.timerSignal,
                        viewModel::setSignal,
                        otherTask = activeTitle.takeIf { timer.isActive && timer.taskId != taskId },
                        onStart = { viewModel.start(taskId) },
                    )

                    Mood.BREAK_TIME -> BreakTimeControls(viewModel)

                    Mood.FOCUSING, Mood.ON_BREAK -> RunningControls(viewModel, timer)
                }
            }
        }
    }
}

/** The clock, read about once a second in every mood, so even a ready dial's end time stays true. */
@Composable
private fun rememberNow(viewModel: FocusViewModel, timer: FocusTimer): State<Instant> =
    produceState(viewModel.now(), timer) {
        while (isActive) {
            value = viewModel.now()
            delay(SECOND_MS)
        }
    }

/** The length of a round still to start: its minutes, the −/+ step, the quick chips, and its name. */
private class LengthPicker(
    val minutes: Int,
    val step: Int,
    val presets: List<Int>,
    val label: String,
    val onChange: (Int) -> Unit,
)

/** The length that can still be changed: the focus while ready, the break while it waits; none while running. */
@Composable
private fun lengthPicker(mood: Mood, settings: Settings, viewModel: FocusViewModel): LengthPicker? = when (mood) {
    Mood.READY -> LengthPicker(
        settings.focusMinutes,
        FOCUS_STEP,
        FocusPresets.FOCUS + settings.focusCustomMinutes,
        stringResource(R.string.focus_label),
        viewModel::setFocusMinutes,
    )

    Mood.BREAK_TIME -> LengthPicker(
        settings.breakMinutes,
        BREAK_STEP,
        FocusPresets.BREAK + settings.breakCustomMinutes,
        stringResource(R.string.focus_break),
        viewModel::setBreakMinutes,
    )

    Mood.FOCUSING, Mood.ON_BREAK -> null
}

/** [RoundCheck]'s touch target is wider than its ring by this much on each side. */
private val CHECK_INSET = 8.dp

/** Lowers the title so its first line centres on the check box's 40dp touch target. */
private val TITLE_NUDGE = 6.dp

/** The task being worked on and its focus so far, with its round box: ticking it off ends the timer too. */
@Composable
private fun TaskHeader(task: Task, focused: TaskFocusTotal, onComplete: () -> Unit) {
    // Pulled back by the touch target's margin, so the ring itself lines up with the content gutter.
    Row(Modifier.fillMaxWidth().padding(top = 4.dp).offset(x = -CHECK_INSET)) {
        RoundCheck(checked = task.completedAt != null, onClick = onComplete, priority = task.priority, size = 24)
        Column(Modifier.weight(1f).padding(start = 4.dp, top = TITLE_NUDGE)) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                focusTotal(focused),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val SETTLED_DIAL = 240.dp
private val RUNNING_DIAL = 280.dp
private val STEP_BUTTON = 44.dp
private val STEP_GAP = 12.dp

/**
 * The dial, sized from the width it gets. While the length can change, −/+ flank it and it stays small
 * enough for all three to fit on a 360dp phone; while a round runs it grows. At break time the length of
 * the focus just done heads it.
 */
@Composable
private fun DialBlock(
    mood: Mood,
    timer: FocusTimer,
    reading: DialReading,
    now: Instant,
    viewModel: FocusViewModel,
    length: LengthPicker?,
) {
    val endsAt = now.plus(reading.left).atZone(viewModel.zone).toLocalTime()
    val caption = captionText(dialCaption(mood, timer), timeLabel(endsAt))
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val done = timer.roundLength.takeIf { mood == Mood.BREAK_TIME }
        if (done != null) {
            Text(
                stringResource(R.string.focus_done_headline, focusDuration(done)),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (length == null) {
                Dial(reading, mood.isBreak, caption, min(RUNNING_DIAL, maxWidth))
            } else {
                val dial = min(SETTLED_DIAL, maxWidth - (STEP_BUTTON + STEP_GAP) * 2)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(STEP_GAP),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepButton(AppIcons.Remove, stringResource(R.string.focus_less, length.label)) {
                        length.onChange(stepDown(length.minutes, length.step))
                    }
                    Dial(reading, mood.isBreak, caption, dial)
                    StepButton(AppIcons.Add, stringResource(R.string.focus_more, length.label)) {
                        length.onChange(stepUp(length.minutes, length.step))
                    }
                }
            }
        }
    }
}

@Composable
private fun captionText(caption: DialCaption, endsAt: String): String = when (caption) {
    DialCaption.UNTIL -> stringResource(R.string.focus_until, endsAt)
    DialCaption.BREAK_UNTIL -> stringResource(R.string.focus_break_until, endsAt)
    DialCaption.PAUSED -> stringResource(R.string.focus_paused)
}

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val TICKS = 12
private const val TICK_ALPHA = 0.5f
private val DIAL_STROKE = 10.dp
private val TICK_GAP = 6.dp
private val TICK_LENGTH = 6.dp
private val TICK_WIDTH = 1.5.dp

/** The time keeps its full size from this dial width up and shrinks with smaller ones. */
private val FULL_TIME_DIAL = 240.dp
private val TIME_SIZE = 68.sp

/**
 * A kitchen-timer dial: a solid arc of the time left (see [DialReading]) on a neutral track, the accent for
 * focus and green for a break, with a tick every five minutes just inside. The time and one caption sit in
 * the middle.
 */
@Composable
private fun Dial(reading: DialReading, isBreak: Boolean, caption: String, size: Dp) {
    val tokens = MaterialTheme.tokens
    val scheme = MaterialTheme.colorScheme
    val sweep by animateFloatAsState(reading.sweep, label = "dial")
    val arc = if (isBreak) tokens.rest else tokens.accent
    val tick = tokens.outline.copy(alpha = TICK_ALPHA)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawDial(sweep, arc, tokens.divider, tick) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                countdown(reading.left),
                // Tabular figures keep the digits from shifting as they count down.
                style = TextStyle(
                    fontSize = TIME_SIZE * (size / FULL_TIME_DIAL).coerceAtMost(1f),
                    fontWeight = FontWeight.Light,
                    fontFeatureSettings = "tnum",
                ),
                color = scheme.onBackground,
            )
            Text(caption, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        }
    }
}

private fun DrawScope.drawDial(sweep: Float, arc: Color, track: Color, tick: Color) {
    val stroke = DIAL_STROKE.toPx()
    val ring = Size(size.minDimension - stroke, size.minDimension - stroke)
    val topLeft = Offset(stroke / 2, stroke / 2)
    val style = Stroke(width = stroke, cap = StrokeCap.Round)
    drawArc(track, 0f, FULL_CIRCLE, useCenter = false, topLeft = topLeft, size = ring, style = style)
    if (sweep > 0f) {
        drawArc(arc, START_ANGLE, FULL_CIRCLE * sweep, useCenter = false, topLeft = topLeft, size = ring, style = style)
    }
    drawTicks(tick, outer = size.minDimension / 2 - stroke - TICK_GAP.toPx())
}

/** Twelve short radial marks, one every five minutes of the hour face, the first at the top. */
private fun DrawScope.drawTicks(color: Color, outer: Float) {
    val inner = outer - TICK_LENGTH.toPx()
    repeat(TICKS) { i ->
        val angle = Math.toRadians((i * FULL_CIRCLE / TICKS).toDouble())
        val direction = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
        drawLine(color, center + direction * inner, center + direction * outer, TICK_WIDTH.toPx(), StrokeCap.Round)
    }
}

/** Ready to focus: the end signal, a note when starting here ends another task's timer, and one button. */
@Composable
private fun ReadyControls(
    signal: TimerSignal,
    onSignal: (TimerSignal) -> Unit,
    otherTask: String?,
    onStart: () -> Unit,
) {
    SignalRow(signal, onSignal)
    Spacer(Modifier.height(8.dp))
    if (otherTask != null) {
        Text(
            stringResource(R.string.focus_other_running, otherTask),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        )
    }
    Buttons(primary = stringResource(R.string.focus_start) to onStart, secondary = null)
}

/** Focus has ended and the break waits: skipping it is one tap. */
@Composable
private fun BreakTimeControls(viewModel: FocusViewModel) {
    Spacer(Modifier.height(8.dp))
    Buttons(
        primary = stringResource(R.string.focus_start_break) to viewModel::startBreak,
        secondary = stringResource(R.string.focus_skip_break) to viewModel::skipBreak,
    )
}

@Composable
private fun RunningControls(viewModel: FocusViewModel, timer: FocusTimer) {
    val pause = if (timer.isPaused) {
        stringResource(R.string.focus_resume) to viewModel::resume
    } else {
        stringResource(R.string.focus_pause) to viewModel::pause
    }
    // Stopping focus keeps the time spent; ending a break goes straight back to ready.
    val stop = if (timer.phase == FocusPhase.BREAK) {
        stringResource(R.string.focus_end_break) to viewModel::skipBreak
    } else {
        stringResource(R.string.focus_stop) to viewModel::stop
    }
    Buttons(primary = pause, secondary = stop)
}

/**
 * Quick lengths in minutes, the picked one filled. The chips share the row's width equally, so four fit
 * on a narrow phone whatever their lengths.
 */
@Composable
private fun Presets(options: List<Int>, value: Int, onPick: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // A custom length equal to a preset (only from a hand-edited backup) shows once.
        options.distinct().forEach { option ->
            val on = value == option
            Text(
                stringResource(R.string.focus_minutes, option),
                style = MaterialTheme.typography.labelLarge,
                color = if (on) scheme.secondary else scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (on) scheme.primaryContainer else scheme.surfaceVariant)
                    .clickable(role = Role.Button) { onPick(option) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
            )
        }
    }
}

/** How a round's end makes itself known, the same choice as in Settings: sound, vibrate or both. */
@Composable
private fun SignalRow(signal: TimerSignal, onPick: (TimerSignal) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.focus_signal),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Row(Modifier.clip(CircleShape).background(scheme.surfaceVariant).padding(3.dp)) {
            TimerSignal.entries.forEach { option ->
                val on = option == signal
                Text(
                    stringResource(signalShort(option)),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) scheme.onSurface else scheme.onSurfaceVariant,
                    modifier = Modifier.clip(CircleShape).background(if (on) scheme.surface else Color.Transparent)
                        .clickable(role = Role.RadioButton) { onPick(option) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun signalShort(signal: TimerSignal): Int = when (signal) {
    TimerSignal.SOUND -> R.string.signal_sound
    TimerSignal.VIBRATE -> R.string.signal_vibrate
    TimerSignal.BOTH -> R.string.signal_both
}

@Composable
private fun StepButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(STEP_BUTTON).clip(CircleShape).background(scheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = scheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Buttons(primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>?) {
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        secondary?.let { (label, action) ->
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(CircleShape).background(scheme.surfaceVariant)
                    .clickable(role = Role.Button, onClick = action).padding(vertical = 16.dp),
            )
        }
        Text(
            primary.first,
            style = MaterialTheme.typography.titleSmall,
            color = scheme.onPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).clip(CircleShape).background(scheme.primary)
                .clickable(role = Role.Button, onClick = primary.second).padding(vertical = 16.dp),
        )
    }
}
