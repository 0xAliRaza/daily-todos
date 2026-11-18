package com.aliraza.dailytasks.ui.focus

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusTimer
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.ui.components.AppIcons
import com.aliraza.dailytasks.ui.theme.tokens
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private const val SECOND_MS = 1_000L
private const val SECONDS_PER_MINUTE = 60
private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private val PRESETS = listOf(15, 25, 50)

@Composable
fun FocusScreen(viewModel: FocusViewModel, padding: PaddingValues) {
    val timer by viewModel.timer.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val title by viewModel.taskTitle.collectAsStateWithLifecycle()
    val now by produceState(viewModel.now(), timer) {
        while (true) {
            value = viewModel.now()
            delay(SECOND_MS)
        }
    }
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Dial(timer, settings, now, title)
        Spacer(Modifier.height(28.dp))
        when (timer.phase) {
            FocusPhase.IDLE -> StartControls(viewModel, settings)

            FocusPhase.WAITING -> Buttons(
                primary = stringResource(R.string.focus_start_next) to viewModel::next,
                secondary = stringResource(R.string.focus_stop) to viewModel::stop,
            )

            else -> RunningControls(viewModel, timer)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.focus_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** The ready ring shows the accent faintly; it turns solid once a round runs. */
private const val READY_ALPHA = 0.3f

@Composable
private fun Dial(timer: FocusTimer, settings: Settings, now: Instant, title: String?) {
    val total = when (timer.phase) {
        FocusPhase.BREAK -> Duration.ofMinutes(settings.breakMinutes.toLong())
        else -> Duration.ofMinutes(settings.focusMinutes.toLong())
    }
    val left = if (timer.phase == FocusPhase.IDLE) total else timer.remaining(now)
    val fraction by animateFloatAsState(progress(left, total), label = "dial")
    // A thin ring: soft while ready, red while focusing, green while resting.
    val tokens = MaterialTheme.tokens
    val arc = when (timer.phase) {
        FocusPhase.IDLE -> tokens.accent.copy(alpha = READY_ALPHA)
        FocusPhase.BREAK -> tokens.dateToday
        else -> tokens.accent
    }
    Box(Modifier.size(264.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            drawArc(tokens.divider, 0f, FULL_CIRCLE, useCenter = false, style = stroke)
            val sweep = FULL_CIRCLE * fraction
            if (sweep > 0f) drawArc(arc, START_ANGLE, sweep, false, style = stroke)
        }
        DialText(phaseLabel(timer), left, title)
    }
}

private fun progress(left: Duration, total: Duration): Float =
    if (total.isZero) 0f else (left.toMillis().toFloat() / total.toMillis()).coerceIn(0f, 1f)

@Composable
private fun DialText(phase: String, left: Duration, title: String?) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(phase, style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        val seconds = left.seconds
        Text(
            "%02d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE),
            // Tabular figures keep the digits from shifting as they count down.
            style = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Light, fontFeatureSettings = "tnum"),
            color = scheme.onBackground,
        )
        if (title != null) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(180.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun phaseLabel(timer: FocusTimer): String = when {
    timer.isPaused -> stringResource(R.string.focus_paused)
    timer.phase == FocusPhase.FOCUS -> stringResource(R.string.focus_progress_focus, timer.round)
    timer.phase == FocusPhase.BREAK -> stringResource(R.string.focus_break)
    timer.phase == FocusPhase.WAITING -> stringResource(R.string.focus_alert_break_done)
    else -> stringResource(R.string.focus_ready)
}

@Composable
private fun StartControls(viewModel: FocusViewModel, settings: Settings) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PRESETS.forEach { minutes ->
            val on = settings.focusMinutes == minutes
            Text(
                stringResource(R.string.focus_minutes, minutes),
                style = MaterialTheme.typography.labelLarge,
                color = if (on) scheme.secondary else scheme.onSurfaceVariant,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (on) scheme.primaryContainer else scheme.surfaceVariant)
                    .clickable(role = Role.Button) { viewModel.setFocusMinutes(minutes) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(scheme.surface)) {
        Stepper(
            stringResource(R.string.focus_label),
            settings.focusMinutes,
            FOCUS_STEP,
        ) { viewModel.setFocusMinutes(it) }
        Stepper(stringResource(R.string.focus_break), settings.breakMinutes, 1, enabled = settings.breaksEnabled) {
            viewModel.setBreakMinutes(it)
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.focus_breaks),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = settings.breaksEnabled,
                onCheckedChange = viewModel::setBreaks,
                colors = SwitchDefaults.colors(checkedTrackColor = scheme.primary),
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    Buttons(primary = stringResource(R.string.focus_start) to viewModel::start, secondary = null)
}

private const val FOCUS_STEP = 5

/** Steps snap to whole multiples of [step], so 1 → 5 → 10 rather than 1 → 6 → 11. */
@Composable
private fun Stepper(label: String, value: Int, step: Int, enabled: Boolean = true, onChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        val lower = ((value - 1) / step) * step
        RoundButton(AppIcons.Remove, stringResource(R.string.focus_less, label), enabled) { onChange(lower) }
        Text(
            stringResource(R.string.focus_minutes, value),
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(76.dp),
        )
        val higher = (value / step + 1) * step
        RoundButton(AppIcons.Add, stringResource(R.string.focus_more, label), enabled) { onChange(higher) }
    }
}

@Composable
private fun RoundButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier.size(
            40.dp,
        ).clip(
            CircleShape,
        ).background(scheme.surfaceVariant).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, label, tint = if (enabled) scheme.onSurface else scheme.outline, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RunningControls(viewModel: FocusViewModel, timer: FocusTimer) {
    val pause = if (timer.isPaused) {
        stringResource(
            R.string.focus_resume,
        ) to viewModel::resume
    } else {
        stringResource(R.string.focus_pause) to viewModel::pause
    }
    if (timer.phase == FocusPhase.BREAK) {
        Buttons(
            primary = stringResource(R.string.focus_skip_break) to viewModel::skipBreak,
            secondary = stringResource(R.string.focus_stop) to viewModel::stop,
        )
    } else {
        Buttons(primary = pause, secondary = stringResource(R.string.focus_stop) to viewModel::stop)
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
