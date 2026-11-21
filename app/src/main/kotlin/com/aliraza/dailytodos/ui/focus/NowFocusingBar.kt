package com.aliraza.dailytodos.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.focus.FocusPhase
import com.aliraza.dailytodos.core.focus.FocusTimer
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.theme.dayHeader
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * What the bar's one button does, through the same timer actions as the timer screen: pause or resume a
 * focus round, or skip the break.
 */
class BarActions(val pause: () -> Unit, val resume: () -> Unit, val skipBreak: () -> Unit)

/** The bar's actions on this ViewModel's timer. */
fun FocusViewModel.barActions(): BarActions = BarActions(::pause, ::resume, ::skipBreak)

/** The bar's one button for [timer]: Pause or Resume while focusing, Skip on a break; none while idle. */
internal enum class BarButton { PAUSE, RESUME, SKIP }

internal fun barButton(timer: FocusTimer): BarButton? = when (timer.phase) {
    FocusPhase.FOCUS -> if (timer.isPaused) BarButton.RESUME else BarButton.PAUSE
    FocusPhase.BREAK, FocusPhase.BREAK_DUE -> BarButton.SKIP
    FocusPhase.IDLE -> null
}

/**
 * The timer at a glance from everywhere else: a rounded bar in the accent container during focus ("Focus ·
 * 18:42" over the task) and the break container on a break ("Break · 4:12", "Next: Call bank"), with a thin
 * line along the bottom that fills as the round goes. A waiting break reads "Break time". A tap goes back
 * to the timer; the button pauses focus or skips the break through [actions], and hides without them.
 */
@Composable
fun NowFocusingBar(
    timer: FocusTimer,
    title: String,
    now: () -> Instant,
    actions: BarActions? = null,
    onOpen: () -> Unit,
) {
    val at by produceState(now(), timer) {
        while (timer.isRunning) {
            value = now()
            delay(TICK_MS)
        }
        value = now()
    }
    val colors = barColors(timer.phase == FocusPhase.FOCUS)
    val progress = timer.progress(at)
    Row(
        Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp).fillMaxWidth().height(BAR_HEIGHT)
            .clip(RoundedCornerShape(BAR_RADIUS)).background(colors.container)
            .drawWithContent {
                drawContent()
                progressLine(colors.ink, progress)
            }
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (timer.phase == FocusPhase.FOCUS) AppIcons.Clock else AppIcons.Cup,
            null,
            tint = colors.content,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                headline(timer, at),
                style = MaterialTheme.typography.dayHeader.merge(TextStyle(fontFeatureSettings = "tnum")),
                color = colors.content,
                maxLines = 1,
            )
            Text(
                if (timer.phase == FocusPhase.FOCUS) title else stringResource(R.string.bar_next, title),
                style = MaterialTheme.typography.meta,
                color = colors.content.copy(alpha = DETAIL_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val button = barButton(timer)
        if (actions != null && button != null) {
            Spacer(Modifier.width(12.dp))
            BarActionButton(button, actions, colors)
        }
    }
}

/** "Focus · 18:42", "Break · 4:12", either one "· Paused", or "Break time" while the break waits. */
@Composable
private fun headline(timer: FocusTimer, at: Instant): String {
    if (timer.phase == FocusPhase.BREAK_DUE) return stringResource(R.string.focus_break_time)
    val status = if (timer.isPaused) stringResource(R.string.focus_paused) else countdown(timer.remaining(at))
    return stringResource(if (timer.phase == FocusPhase.FOCUS) R.string.bar_focus else R.string.bar_break, status)
}

@Composable
private fun BarActionButton(button: BarButton, actions: BarActions, colors: BarColors) {
    val (icon, label, action) = when (button) {
        BarButton.PAUSE -> Triple(AppIcons.Pause, R.string.focus_pause, actions.pause)
        BarButton.RESUME -> Triple(AppIcons.Play, R.string.focus_resume, actions.resume)
        BarButton.SKIP -> Triple(AppIcons.Skip, R.string.focus_skip_break, actions.skipBreak)
    }
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(colors.ink).clickable(role = Role.Button, onClick = action),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, stringResource(label), tint = colors.onInk, modifier = Modifier.size(24.dp))
    }
}

/** The bar's container and text, and the solid [ink] of its button and progress line with what sits on it. */
private data class BarColors(val container: Color, val content: Color, val ink: Color, val onInk: Color)

@Composable
private fun barColors(focusing: Boolean): BarColors = with(MaterialTheme.tokens) {
    if (focusing) {
        BarColors(accentContainer, onAccentContainer, accent, onAccent)
    } else {
        BarColors(restContainer, onRestContainer, rest, onRest)
    }
}

/** The round's line along the bar's bottom edge, filled in [ink] up to [progress]. */
private fun DrawScope.progressLine(ink: Color, progress: Float) {
    val height = PROGRESS_HEIGHT.toPx()
    drawRect(ink, topLeft = Offset(0f, size.height - height), size = Size(size.width * progress, height))
}

private val BAR_HEIGHT = 60.dp
private val BAR_RADIUS = 18.dp
private val PROGRESS_HEIGHT = 3.dp
private const val DETAIL_ALPHA = 0.85f
private const val TICK_MS = 1_000L
