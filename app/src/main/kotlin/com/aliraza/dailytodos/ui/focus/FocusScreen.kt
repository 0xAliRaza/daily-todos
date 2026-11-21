package com.aliraza.dailytodos.ui.focus

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusPresets
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.Task
import com.aliraza.dailytodos.core.model.TimerSignal
import com.aliraza.dailytodos.data.TaskFocusTotal
import com.aliraza.dailytodos.ui.components.AppButton
import com.aliraza.dailytodos.ui.components.AppButtons
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.ButtonColors
import com.aliraza.dailytodos.ui.components.PillChoice
import com.aliraza.dailytodos.ui.components.PillTone
import com.aliraza.dailytodos.ui.components.RoundCheck
import com.aliraza.dailytodos.ui.components.rememberTick
import com.aliraza.dailytodos.ui.components.timeLabel
import com.aliraza.dailytodos.ui.theme.body
import com.aliraza.dailytodos.ui.theme.button
import com.aliraza.dailytodos.ui.theme.dayHeader
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.sheetTitle
import com.aliraza.dailytodos.ui.theme.timer
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.Instant
import kotlin.math.cos
import kotlin.math.sin

private const val SECOND_MS = 1_000L
private const val FOCUS_STEP = 5
private const val BREAK_STEP = 1

/** The page's side gutter; the dial row alone runs edge to edge so its steppers fit beside it. */
private val GUTTER = 20.dp

/**
 * One task's focus timer: one screen in four moods (see [Mood]). The task sits at the top, the dial in the
 * middle and the controls at the bottom, in thumb reach; on a short screen they stack and scroll. On a
 * break the screen's accent is the break green throughout. Leaving the screen never stops the timer.
 * [onComplete] ticks the task off, which also stops its timer. [onGone] leaves once the task is gone.
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
    val current = task
    LaunchedEffect(current?.deletedAt) { if (current?.deletedAt != null) onGone() }
    if (current == null) return
    val now by rememberNow(viewModel, timer)
    val mood = moodOf(timer, taskId)
    val length = lengthPicker(mood, settings, viewModel)
    val tint = focusTint(mood.isBreak)
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = GUTTER)) {
                KeepScreenOnToggle(
                    settings.keepScreenOn,
                    viewModel::setKeepScreenOn,
                    Modifier.align(Alignment.End),
                    tint.pill,
                )
                TaskHeader(current, focusHeadline(mood, focused.focused, focused.sessions), onComplete)
            }
            Middle(mood, timer, settings, now, viewModel, length, focused, tint)
            Controls(mood, timer, settings, now, viewModel, taskId, tint)
        }
    }
}

/** The dial with what goes round it: the break's chip above, presets or the day's focus below. */
@Composable
private fun Middle(
    mood: Mood,
    timer: FocusTimer,
    settings: Settings,
    now: Instant,
    viewModel: FocusViewModel,
    length: LengthPicker?,
    focused: TaskFocusTotal,
    tint: FocusTint,
) {
    val reading = dialReading(mood, timer, settings, now)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MIDDLE_GAP),
    ) {
        if (mood == Mood.BREAK_TIME) RoundDoneChip()
        val caption = captionText(dialCaption(mood, timer), endsAt(now, reading, viewModel))
        DialRow(reading, caption, tint, length)
        if (length != null) {
            PillChoice(
                length.presets.distinct(),
                length.minutes,
                { stringResource(R.string.focus_minutes, it) },
                length.onChange,
                Modifier.padding(horizontal = GUTTER),
                tone = tint.pill,
            )
        } else {
            val stats by viewModel.stats.collectAsStateWithLifecycle()
            DayLine(stats.today, focused.sessions)
        }
    }
}

private val MIDDLE_GAP = 22.dp

/** The controls at the bottom, in thumb reach, for [mood]. */
@Composable
private fun Controls(
    mood: Mood,
    timer: FocusTimer,
    settings: Settings,
    now: Instant,
    viewModel: FocusViewModel,
    taskId: String,
    tint: FocusTint,
) {
    val activeTitle by viewModel.activeTitle.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (mood) {
            Mood.READY -> ReadyControls(
                settings.timerSignal,
                viewModel::setSignal,
                endsAt = endsAt(now, dialReading(mood, timer, settings, now), viewModel),
                otherTask = activeTitle.takeIf { timer.isActive && timer.taskId != taskId },
                onStart = { viewModel.start(taskId) },
            )

            Mood.BREAK_TIME -> ButtonPair(
                Action(stringResource(R.string.focus_skip_break), AppIcons.Skip, viewModel::skipBreak),
                Action(stringResource(R.string.focus_start_break), AppIcons.Play, viewModel::startBreak),
                tint,
            )

            Mood.FOCUSING, Mood.ON_BREAK -> RunningControls(viewModel, timer, tint)
        }
    }
}

/** When the round on the dial ends, read from [now] and the time it has left, in the app's clock format. */
@Composable
private fun endsAt(now: Instant, reading: DialReading, viewModel: FocusViewModel): String =
    timeLabel(now.plus(reading.left).atZone(viewModel.zone).toLocalTime())

/** The colours the screen picks from: the accent while focusing, the break green on a break. */
private data class FocusTint(val arc: Color, val pill: PillTone, val primary: ButtonColors, val isBreak: Boolean)

@Composable
private fun focusTint(isBreak: Boolean): FocusTint = if (isBreak) {
    FocusTint(MaterialTheme.tokens.rest, PillTone.REST, AppButtons.rest(), isBreak = true)
} else {
    FocusTint(MaterialTheme.tokens.accent, PillTone.ACCENT, AppButtons.primary(), isBreak = false)
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

/** The length that can still be changed: the focus while ready, the break while it waits; none while running. */
@Composable
private fun lengthPicker(mood: Mood, settings: Settings, viewModel: FocusViewModel): LengthPicker? = when (mood) {
    Mood.READY -> LengthPicker(
        settings.focusMinutes,
        Settings.FOCUS_MINUTES_RANGE,
        FOCUS_STEP,
        FocusPresets.FOCUS + settings.focusCustomMinutes,
        stringResource(R.string.focus_label),
        viewModel::setFocusMinutes,
    )

    Mood.BREAK_TIME -> LengthPicker(
        settings.breakMinutes,
        Settings.BREAK_MINUTES_RANGE,
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
private val TITLE_NUDGE = 5.dp

/** The task being worked on and its focus so far, with its round box: ticking it off ends the timer too. */
@Composable
private fun TaskHeader(task: Task, headline: FocusHeadline, onComplete: () -> Unit) {
    // Pulled back by the touch target's margin, so the ring itself lines up with the content gutter.
    Row(Modifier.fillMaxWidth().padding(top = 4.dp).offset(x = -CHECK_INSET)) {
        RoundCheck(checked = task.completedAt != null, onClick = onComplete, priority = task.priority, size = 24.dp)
        Column(Modifier.weight(1f).padding(start = 6.dp, top = TITLE_NUDGE)) {
            Text(
                task.title,
                style = MaterialTheme.typography.sheetTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                headlineText(headline),
                style = MaterialTheme.typography.body,
                color = MaterialTheme.tokens.ink2,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun headlineText(headline: FocusHeadline): String = when (headline) {
    FocusHeadline.None -> stringResource(R.string.focus_none_yet)

    is FocusHeadline.Total -> stringResource(
        R.string.task_focus_total,
        focusDuration(headline.focused),
        pluralStringResource(R.plurals.focus_rounds, headline.rounds, headline.rounds),
    )

    is FocusHeadline.Round -> headline.focusedBefore?.let {
        stringResource(R.string.focus_round_so_far, headline.round, focusDuration(it))
    } ?: stringResource(R.string.focus_round_number, headline.round)
}

/** Focus has ended and the break waits: a green chip says so above the dial. */
@Composable
private fun RoundDoneChip() {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.height(32.dp).clip(CircleShape).background(tokens.restContainer).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(AppIcons.Cup, null, tint = tokens.onRestContainer, modifier = Modifier.size(18.dp))
        Text(
            stringResource(R.string.focus_round_done),
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.Bold,
            color = tokens.onRestContainer,
        )
    }
}

/** While a round runs: today's focus over every task, and the rounds this task has had. */
@Composable
private fun DayLine(today: Duration, rounds: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Labelled(stringResource(R.string.focus_today_label), focusDuration(today))
        Labelled(
            stringResource(R.string.focus_this_task_label),
            pluralStringResource(R.plurals.focus_rounds, rounds, rounds),
        )
    }
}

@Composable
private fun Labelled(label: String, value: String) {
    val tokens = MaterialTheme.tokens
    Text(
        buildAnnotatedString {
            append(label)
            append(' ')
            withStyle(SpanStyle(color = tokens.ink, fontWeight = FontWeight.Bold)) { append(value) }
        },
        style = MaterialTheme.typography.meta,
        color = tokens.ink2,
    )
}

private val SETTLED_DIAL = 260.dp
private val RUNNING_DIAL = 300.dp
private val STEP_BUTTON = 52.dp
private val STEP_GAP = 4.dp

/**
 * The dial, sized from the width it gets. While the length can change, −/+ flank it and it stays small
 * enough for all three to fit on a 360dp phone; while a round runs it grows.
 */
@Composable
private fun DialRow(reading: DialReading, caption: Caption, tint: FocusTint, length: LengthPicker?) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (length == null) {
            Dial(reading, tint.arc, caption, min(RUNNING_DIAL, maxWidth - GUTTER * 2))
        } else {
            val dial = min(SETTLED_DIAL, maxWidth - (STEP_BUTTON + STEP_GAP) * 2)
            Row(
                horizontalArrangement = Arrangement.spacedBy(STEP_GAP),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton(
                    stringResource(R.string.focus_step_less, length.step),
                    pluralStringResource(R.plurals.focus_minutes_less, length.step, length.step),
                ) { length.onChange(stepDown(length.minutes, length.step)) }
                Dial(reading, tint.arc, caption, dial, Modifier.dialInput(rememberDialInput(length)))
                StepButton(
                    stringResource(R.string.focus_step_more, length.step),
                    pluralStringResource(R.plurals.focus_minutes_more, length.step, length.step),
                ) { length.onChange(stepUp(length.minutes, length.step)) }
            }
        }
    }
}

/** The caption's words and whether they take the break green, as "Break" does at break time. */
private data class Caption(val text: String, val tinted: Boolean = false)

@Composable
private fun captionText(caption: DialCaption, endsAt: String): Caption = when (caption) {
    DialCaption.FOCUS_ROUND -> Caption(stringResource(R.string.focus_round_caption))
    DialCaption.BREAK -> Caption(stringResource(R.string.focus_break), tinted = true)
    DialCaption.ENDS -> Caption(stringResource(R.string.focus_ends, endsAt))
    DialCaption.PAUSED -> Caption(stringResource(R.string.focus_paused))
}

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val TICKS = 12

/** The ring's stroke and its tick marks, as shares of the dial's width (the board draws 14 on 280). */
private const val STROKE_SHARE = 14f / 280f
private const val TICK_OUTER_SHARE = 113f / 280f
private const val TICK_INNER_SHARE = 106f / 280f
private val TICK_WIDTH = 2.dp

/** The time keeps its full size from this dial width up and shrinks with smaller ones. */
private val FULL_TIME_DIAL = 260.dp

/**
 * The dial: a solid arc of the round's progress (see [DialReading]) on the fill's track, in [arc], with a
 * tick every five minutes just inside. The time and one caption sit in the middle. [modifier] carries the
 * touch input while the length can be set.
 */
@Composable
private fun Dial(reading: DialReading, arc: Color, caption: Caption, size: Dp, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.tokens
    val sweep by animateFloatAsState(reading.sweep, label = "dial")
    val timer = MaterialTheme.typography.timer
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawDial(sweep, arc, tokens.fill, tokens.ink3) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                countdown(reading.left),
                // Tabular figures keep the digits from shifting as they count down.
                style = timer.copy(
                    fontSize = timer.fontSize * (size / FULL_TIME_DIAL).coerceAtMost(1f),
                    lineHeight = timer.lineHeight * (size / FULL_TIME_DIAL).coerceAtMost(1f),
                    fontFeatureSettings = "tnum",
                ),
                color = tokens.ink,
            )
            Text(
                caption.text,
                style = MaterialTheme.typography.dayHeader,
                fontWeight = FontWeight.SemiBold,
                color = if (caption.tinted) arc else tokens.ink2,
            )
        }
    }
}

private fun DrawScope.drawDial(sweep: Float, arc: Color, track: Color, tick: Color) {
    val stroke = size.minDimension * STROKE_SHARE
    val ring = Size(size.minDimension - stroke, size.minDimension - stroke)
    val topLeft = Offset(stroke / 2, stroke / 2)
    val style = Stroke(width = stroke, cap = StrokeCap.Round)
    drawArc(track, 0f, FULL_CIRCLE, useCenter = false, topLeft = topLeft, size = ring, style = style)
    if (sweep > 0f) {
        drawArc(arc, START_ANGLE, FULL_CIRCLE * sweep, useCenter = false, topLeft = topLeft, size = ring, style = style)
    }
    drawTicks(tick, inner = size.minDimension * TICK_INNER_SHARE, outer = size.minDimension * TICK_OUTER_SHARE)
}

/** Twelve short radial marks, one every five minutes of the hour face, the first at the top. */
private fun DrawScope.drawTicks(color: Color, inner: Float, outer: Float) {
    repeat(TICKS) { i ->
        val angle = Math.toRadians((i * FULL_CIRCLE / TICKS).toDouble())
        val direction = Offset(sin(angle).toFloat(), -cos(angle).toFloat())
        drawLine(color, center + direction * inner, center + direction * outer, TICK_WIDTH.toPx(), StrokeCap.Round)
    }
}

/**
 * Ready to focus: the end signal, when the round would end, a note when starting here ends another task's
 * timer, and one button. Nothing starts until it is tapped.
 */
@Composable
private fun ReadyControls(
    signal: TimerSignal,
    onSignal: (TimerSignal) -> Unit,
    endsAt: String,
    otherTask: String?,
    onStart: () -> Unit,
) {
    val tokens = MaterialTheme.tokens
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.settings_signal),
            style = MaterialTheme.typography.button,
            color = tokens.ink2,
            maxLines = 1,
        )
        PillChoice(TimerSignal.entries, signal, { stringResource(signalLabel(it)) }, onSignal, Modifier.weight(1f))
    }
    CentredMeta(stringResource(R.string.focus_ends, endsAt), Modifier.padding(top = 4.dp))
    otherTask?.let { CentredMeta(stringResource(R.string.focus_other_running, it)) }
    val tick = rememberTick(HapticFeedbackType.Confirm)
    AppButton(
        stringResource(R.string.focus_start),
        onClick = {
            tick()
            onStart()
        },
        colors = AppButtons.primary(),
        modifier = Modifier.fillMaxWidth().height(TIMER_BUTTON),
        icon = AppIcons.Play,
    )
}

@Composable
private fun CentredMeta(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.meta,
        color = MaterialTheme.tokens.ink2,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Pause or Resume as the main button; Stop keeps the focus spent, End break goes straight back to ready. */
@Composable
private fun RunningControls(viewModel: FocusViewModel, timer: FocusTimer, tint: FocusTint) {
    val pause = if (timer.isPaused) {
        Action(stringResource(R.string.focus_resume), AppIcons.Play, viewModel::resume)
    } else {
        Action(stringResource(R.string.focus_pause), AppIcons.Pause, viewModel::pause)
    }
    val stop = if (timer.phase == FocusPhase.BREAK) {
        Action(stringResource(R.string.focus_end_break), AppIcons.Skip, viewModel::skipBreak)
    } else {
        Action(stringResource(R.string.focus_stop), AppIcons.Stop, viewModel::stop)
    }
    ButtonPair(stop, pause, tint)
}

/** The name of an end signal, here and in Settings. */
internal fun signalLabel(signal: TimerSignal): Int = when (signal) {
    TimerSignal.SOUND -> R.string.signal_sound
    TimerSignal.VIBRATE -> R.string.signal_vibrate
    TimerSignal.BOTH -> R.string.signal_both
}

/** A −/+ stepper beside the dial: its [text] ("−5") on the fill, with a spoken [label]. */
@Composable
private fun StepButton(text: String, label: String, onClick: () -> Unit) {
    val tokens = MaterialTheme.tokens
    val tick = rememberTick()
    Box(
        Modifier.size(STEP_BUTTON).clip(CircleShape).background(tokens.fill)
            .clickable(role = Role.Button) {
                tick()
                onClick()
            }
            .semantics(mergeDescendants = true) { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.Bold,
            color = tokens.ink,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** A button's words, icon and what a tap does. */
private class Action(val text: String, val icon: ImageVector, val onClick: () -> Unit)

/** The timer's buttons are a size up from the app's 48dp, as the screen's one job. */
private val TIMER_BUTTON = 56.dp
private const val PRIMARY_WEIGHT = 1.4f

/** A quiet [secondary] on the fill beside a wider [primary] in the screen's tint. */
@Composable
private fun ButtonPair(secondary: Action, primary: Action, tint: FocusTint) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppButton(
            secondary.text,
            secondary.onClick,
            AppButtons.neutral(),
            Modifier.weight(1f).height(TIMER_BUTTON),
            icon = secondary.icon,
        )
        AppButton(
            primary.text,
            primary.onClick,
            tint.primary,
            Modifier.weight(PRIMARY_WEIGHT).height(TIMER_BUTTON),
            icon = primary.icon,
        )
    }
}
