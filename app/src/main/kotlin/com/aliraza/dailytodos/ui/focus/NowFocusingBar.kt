package com.aliraza.dailytodos.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
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
import com.aliraza.dailytodos.ui.theme.tokens
import kotlinx.coroutines.delay
import java.time.Instant

/**
 * The timer at a glance from everywhere else: its task and the countdown, the accent during focus and
 * green for a break, with a thin line along the top that fills as the round goes. A waiting break reads
 * "Break time". A tap goes back to the timer.
 */
@Composable
fun NowFocusingBar(timer: FocusTimer, title: String, now: () -> Instant, onOpen: () -> Unit) {
    val tokens = MaterialTheme.tokens
    val scheme = MaterialTheme.colorScheme
    val at by produceState(now(), timer) {
        while (timer.isRunning) {
            value = now()
            delay(TICK_MS)
        }
        value = now()
    }
    val focusing = timer.phase == FocusPhase.FOCUS
    val status = when {
        timer.phase == FocusPhase.BREAK_DUE -> stringResource(R.string.focus_break_time)
        timer.isPaused -> stringResource(R.string.focus_paused)
        else -> countdown(timer.remaining(at))
    }
    val ink = if (focusing) tokens.accent else tokens.rest
    val progress = timer.progress(at)
    Row(
        Modifier.fillMaxWidth().background(scheme.surface).drawWithContent {
            drawContent()
            progressLine(tokens.divider, ink, progress)
        }.clickable(role = Role.Button, onClick = onOpen)
            .heightIn(min = 48.dp).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Timer, null, tint = ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            status,
            style = MaterialTheme.typography.labelLarge.merge(TextStyle(fontFeatureSettings = "tnum")),
            color = if (focusing) tokens.accentText else scheme.onSurface,
        )
    }
}

/** The round's line along the bar's top edge: a hairline track, filled in [ink] up to [progress]. */
private fun DrawScope.progressLine(track: Color, ink: Color, progress: Float) {
    val height = ProgressHeight.toPx()
    drawRect(track, size = Size(size.width, height))
    drawRect(ink, size = Size(size.width * progress, height))
}

private val ProgressHeight = 2.dp

private const val TICK_MS = 1_000L
