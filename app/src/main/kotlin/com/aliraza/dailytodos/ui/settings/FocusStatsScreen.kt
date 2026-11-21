package com.aliraza.dailytodos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.focus.DayFocus
import com.aliraza.dailytodos.core.focus.FocusStats
import com.aliraza.dailytodos.core.focus.FunComparison
import com.aliraza.dailytodos.core.focus.FunFact
import com.aliraza.dailytodos.core.focus.FunFacts
import com.aliraza.dailytodos.core.focus.TaskFocus
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.EmptyState
import com.aliraza.dailytodos.ui.components.LocalWeekStart
import com.aliraza.dailytodos.ui.components.rowDivider
import com.aliraza.dailytodos.ui.components.weekdayShort
import com.aliraza.dailytodos.ui.focus.FocusViewModel
import com.aliraza.dailytodos.ui.focus.focusDuration
import com.aliraza.dailytodos.ui.theme.barTitle
import com.aliraza.dailytodos.ui.theme.body
import com.aliraza.dailytodos.ui.theme.groupLabel
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.statHeadline
import com.aliraza.dailytodos.ui.theme.tab
import com.aliraza.dailytodos.ui.theme.tokens
import java.time.Duration

/**
 * Focus time, plainly counted and with no cards: the all-time total as the headline, today, this week and
 * the sessions, the last seven days as labelled bars, the tasks it went to, and a comparison in small type.
 */
@Composable
fun FocusStatsScreen(viewModel: FocusViewModel, padding: PaddingValues) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    if (stats.sessions == 0) {
        EmptyState(
            stringResource(R.string.stats_empty_title),
            stringResource(R.string.stats_empty_body),
            padding,
            icon = AppIcons.Timer,
        )
        return
    }
    val comparison = remember(stats.total) { FunFacts.pick(stats.total, viewModel.today()) }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Headline(stats)
        Trio(stats)
        Week(stats.lastDays)
        GroupLabel(R.string.stats_top_all_time)
        TopTasks(stats.topTasks)
        comparison?.let { Comparison(it) }
    }
}

@Composable
private fun Headline(stats: FocusStats) {
    Column(Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, top = 14.dp)) {
        Text(
            stringResource(R.string.stats_all_time_label).uppercase(LocalConfiguration.current.locales[0]),
            style = MaterialTheme.typography.groupLabel,
            color = MaterialTheme.tokens.ink2,
        )
        Text(
            focusDuration(stats.total),
            style = MaterialTheme.typography.statHeadline,
            color = MaterialTheme.tokens.ink,
        )
    }
}

/** Today, this week and the sessions, side by side over a hairline. */
@Composable
private fun Trio(stats: FocusStats) {
    Row(
        Modifier.fillMaxWidth().rowDivider(MaterialTheme.tokens.hairline, 0.dp)
            .padding(start = GUTTER, end = GUTTER, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Figure(focusDuration(stats.today), stringResource(R.string.stats_today))
        Figure(focusDuration(stats.thisWeek(LocalWeekStart.current)), stringResource(R.string.stats_week))
        Figure(stats.sessions.toString(), stringResource(R.string.stats_sessions))
    }
}

@Composable
private fun RowScope.Figure(value: String, label: String) {
    Column(Modifier.weight(1f)) {
        Text(value, style = MaterialTheme.typography.barTitle, color = MaterialTheme.tokens.ink, maxLines = 1)
        Text(label, style = MaterialTheme.typography.meta, color = MaterialTheme.tokens.ink2)
    }
}

private val CHART_HEIGHT = 150.dp
private val BAR_MAX = 92.dp
private val BAR_STUB = 2.dp
private val BAR_GAP = 10.dp
private val BAR_RADIUS = 6.dp

/**
 * Seven bars, oldest first, each with its minutes above it and its weekday below; today's in the accent.
 * The busiest day sets the height and an empty day shows a stub.
 */
@Composable
private fun Week(days: List<DayFocus>) {
    val tokens = MaterialTheme.tokens
    val most = days.maxOf { it.focused.toMillis() }.coerceAtLeast(1)
    val summary = stringResource(R.string.stats_last_days)
    Column(Modifier.semantics(mergeDescendants = true) { contentDescription = summary }) {
        Row(
            Modifier.fillMaxWidth().height(CHART_HEIGHT).rowDivider(tokens.hairline, 0.dp)
                .padding(start = GUTTER, end = GUTTER, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEachIndexed { i, day ->
                Bar(day, today = i == days.lastIndex, share = day.focused.toMillis().toFloat() / most)
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(BAR_GAP),
        ) {
            days.forEachIndexed { i, day ->
                Text(
                    weekdayShort(day.day),
                    style = MaterialTheme.typography.tab,
                    color = if (i == days.lastIndex) tokens.ink else tokens.ink2,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RowScope.Bar(day: DayFocus, today: Boolean, share: Float) {
    val tokens = MaterialTheme.tokens
    Column(
        Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            barValue(day.focused),
            style = MaterialTheme.typography.groupLabel.copy(letterSpacing = 0.sp),
            color = if (today) tokens.ink else tokens.ink2,
            maxLines = 1,
            softWrap = false,
        )
        val height = if (day.focused.isZero) BAR_STUB else (BAR_MAX * share).coerceAtLeast(BAR_STUB)
        Box(
            Modifier.fillMaxWidth().height(height)
                .clip(RoundedCornerShape(topStart = BAR_RADIUS, topEnd = BAR_RADIUS))
                .background(if (today) tokens.accent else tokens.ink3),
        )
    }
}

/** A bar's short label: "0", "40m", "2h" or "1h 20m". */
@Composable
private fun barValue(focused: Duration): String {
    val minutes = focused.toMinutes()
    val hours = (minutes / MINUTES_PER_HOUR).toInt()
    val rest = (minutes % MINUTES_PER_HOUR).toInt()
    return when {
        minutes == 0L -> "0"
        hours == 0 -> stringResource(R.string.stats_bar_minutes, rest)
        rest == 0 -> stringResource(R.string.stats_bar_hours, hours)
        else -> stringResource(R.string.stats_bar_hours_minutes, hours, rest)
    }
}

private const val MINUTES_PER_HOUR = 60L

/** "That’s as long as 72 journeys of sunlight to Earth.", small, under everything else. */
@Composable
private fun Comparison(comparison: FunComparison) {
    val count = comparison.count.toInt()
    Text(
        stringResource(R.string.stats_fact_line, count, pluralStringResource(factName(comparison.fact), count)),
        style = MaterialTheme.typography.meta,
        color = MaterialTheme.tokens.ink2,
        modifier = Modifier.padding(start = GUTTER, end = GUTTER, top = 14.dp),
    )
}

private fun factName(fact: FunFact): Int = when (fact) {
    FunFact.MOON_LASER -> R.plurals.fact_moon_laser
    FunFact.SUNLIGHT -> R.plurals.fact_sunlight
    FunFact.ISS_ORBIT -> R.plurals.fact_iss_orbit
    FunFact.MAYFLY -> R.plurals.fact_mayfly
    FunFact.APOLLO_11 -> R.plurals.fact_apollo_11
    FunFact.MOON_ORBIT -> R.plurals.fact_moon_orbit
}

@Composable
private fun TopTasks(tasks: List<TaskFocus>) {
    val tokens = MaterialTheme.tokens
    tasks.forEach { task ->
        Row(
            Modifier.fillMaxWidth().rowDivider(tokens.hairline, GUTTER).padding(horizontal = GUTTER, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                task.title ?: stringResource(R.string.stats_deleted),
                style = MaterialTheme.typography.body,
                color = if (task.title == null) tokens.ink2 else tokens.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            Text(
                focusDuration(task.focused),
                style = MaterialTheme.typography.body.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.SemiBold,
                color = tokens.ink,
            )
        }
    }
}
