package com.aliraza.dailytodos.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.reminder.Lateness
import com.aliraza.dailytodos.core.reminder.lateness
import com.aliraza.dailytodos.reminder.canPostNotifications
import com.aliraza.dailytodos.ui.components.AppButton
import com.aliraza.dailytodos.ui.components.AppButtons
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.PrimaryButton
import com.aliraza.dailytodos.ui.components.dayLabel
import com.aliraza.dailytodos.ui.components.rowDivider
import com.aliraza.dailytodos.ui.components.timeLabel
import com.aliraza.dailytodos.ui.focus.focusDuration
import com.aliraza.dailytodos.ui.theme.barTitle
import com.aliraza.dailytodos.ui.theme.body
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.tokens
import java.time.LocalDate
import java.time.ZoneId
import android.provider.Settings as AndroidSettings

/** The reminder checks, run again whenever the app comes back to the front. Settings shows their summary. */
@Composable
internal fun rememberHealthChecks(): List<Check> {
    val context = LocalContext.current
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumed++ }
    return remember(resumed) { checks(context) }
}

/** What reminders depend on, in the order the screen lists them: the most common trouble first. */
internal enum class CheckKind { BATTERY, NOTIFICATIONS, EXACT_ALARMS }

/** One check's result, and how to fix it when it fails. */
internal data class Check(val kind: CheckKind, val ok: Boolean, val fix: () -> Unit)

/** The words and icon of each check: its name, its value either way, and the fix's explanation and button. */
private class CheckText(
    val title: Int,
    val icon: ImageVector,
    val okValue: Int,
    val badValue: Int,
    val problem: Int,
    val fixLead: Int,
    val fixBody: Int,
    val fixButton: Int,
)

private fun textOf(kind: CheckKind): CheckText = when (kind) {
    CheckKind.BATTERY -> CheckText(
        R.string.settings_battery,
        AppIcons.Battery,
        R.string.settings_battery_ok,
        R.string.health_value_restricted,
        R.string.health_problem_battery,
        R.string.health_fix_battery_lead,
        R.string.health_fix_battery_body,
        R.string.health_open_battery,
    )

    CheckKind.NOTIFICATIONS -> CheckText(
        R.string.health_notifications,
        AppIcons.Bell,
        R.string.health_allowed,
        R.string.health_value_off,
        R.string.health_problem_notifications,
        R.string.health_fix_notifications_lead,
        R.string.health_fix_notifications_body,
        R.string.health_open_notifications,
    )

    CheckKind.EXACT_ALARMS -> CheckText(
        R.string.health_exact,
        AppIcons.Alarm,
        R.string.health_allowed,
        R.string.health_value_not_allowed,
        R.string.health_problem_exact,
        R.string.health_fix_exact_lead,
        R.string.health_fix_exact_body,
        R.string.health_open_exact,
    )
}

/** What the Settings banner says is wrong: the first failing check's problem. */
internal fun problemOf(check: Check): Int = textOf(check.kind).problem

/**
 * Every permission and battery setting reminders depend on, with the fix first when one fails, then the
 * checks, a quiet test, the maker's steps and the history.
 */
@Composable
fun HealthScreen(viewModel: SettingsViewModel, padding: PaddingValues) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    Messages(viewModel)
    val checks = rememberHealthChecks()
    val appName = stringResource(R.string.app_name)
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        val failing = checks.filterNot { it.ok }
        failing.firstOrNull()?.let { FixFirst(it, failing.size) } ?: AllSet()
        Group(R.string.health_checks) {
            checks.forEach { CheckRow(it) }
            DndRow(settings.dndOverride)
        }
        TestRow { viewModel.sendTestReminder() }
        Group(R.string.health_maker_steps) { MakerSteps(appName) }
        Group(R.string.health_history) {
            if (history.isEmpty()) {
                Text(
                    stringResource(R.string.health_history_empty),
                    style = MaterialTheme.typography.body,
                    color = MaterialTheme.tokens.ink2,
                    modifier = Modifier.padding(horizontal = GUTTER, vertical = 12.dp),
                )
            }
            history.forEach { HistoryRow(it, viewModel.zone) }
        }
        Text(
            stringResource(R.string.health_history_note),
            style = MaterialTheme.typography.meta,
            color = MaterialTheme.tokens.ink2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}

/** The one thing to do first: how many checks fail, what the first one means, and its fix as the main button. */
@Composable
private fun FixFirst(check: Check, count: Int) {
    val tokens = MaterialTheme.tokens
    val text = textOf(check.kind)
    Column(
        Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Verdict(AppIcons.Alert, pluralStringResource(R.plurals.health_to_fix, count, count), tokens.danger)
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(text.fixLead)) }
                append(' ')
                append(stringResource(text.fixBody))
            },
            style = MaterialTheme.typography.body,
            color = tokens.ink,
        )
        PrimaryButton(stringResource(text.fixButton), check.fix, Modifier.fillMaxWidth().padding(top = 4.dp))
    }
}

/** Every check passes: a green word and one line saying what that means. */
@Composable
private fun AllSet() {
    val tokens = MaterialTheme.tokens
    Column(
        Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, top = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Verdict(AppIcons.Check, stringResource(R.string.health_all_set), tokens.rest)
        Text(stringResource(R.string.health_all_set_body), style = MaterialTheme.typography.body, color = tokens.ink)
    }
}

@Composable
private fun Verdict(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
        Text(text, style = MaterialTheme.typography.barTitle, fontWeight = FontWeight.ExtraBold, color = color)
    }
}

/** A check with its icon and value: a failing one in danger, tappable to fix; a passing one with a tick. */
@Composable
private fun CheckRow(check: Check) {
    val tokens = MaterialTheme.tokens
    val text = textOf(check.kind)
    val ink = if (check.ok) tokens.ink2 else tokens.danger
    InfoRow(
        text.icon,
        ink,
        stringResource(text.title),
        stringResource(if (check.ok) text.okValue else text.badValue),
        valueColor = ink,
        valueBold = !check.ok,
        ticked = check.ok,
        onClick = check.fix.takeIf { !check.ok },
    )
}

/** Whether reminders break through Do Not Disturb: a setting, not a check, so it never counts as a fix. */
@Composable
private fun DndRow(dndOverride: Boolean) {
    val ink2 = MaterialTheme.tokens.ink2
    InfoRow(
        AppIcons.Moon,
        ink2,
        stringResource(R.string.health_dnd),
        stringResource(if (dndOverride) R.string.health_dnd_through else R.string.health_dnd_quiet),
        valueColor = ink2,
        valueBold = false,
        ticked = dndOverride,
        onClick = null,
    )
}

private val CHECK_ROW = 48.dp
private val CHECK_TEXT_START = 56.dp

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    valueColor: Color,
    valueBold: Boolean,
    ticked: Boolean,
    onClick: (() -> Unit)?,
) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.fillMaxWidth().rowDivider(tokens.hairline, CHECK_TEXT_START)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .heightIn(min = CHECK_ROW).padding(horizontal = GUTTER),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        Text(title, style = MaterialTheme.typography.body, color = tokens.ink, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.body,
            fontWeight = if (valueBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor,
        )
        if (ticked) Icon(AppIcons.Check, null, tint = tokens.ink2, modifier = Modifier.size(14.dp))
    }
}

/** The test, kept quiet: a neutral button and what it does. */
@Composable
private fun TestRow(onSend: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppButton(stringResource(R.string.health_send_test), onSend, AppButtons.neutral(), icon = AppIcons.Send)
        Text(
            stringResource(R.string.health_test_hint),
            style = MaterialTheme.typography.meta,
            color = MaterialTheme.tokens.ink2,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MakerSteps(appName: String) {
    val steps = stringArrayResource(makerSteps()).map { it.format(appName) }
    Column(Modifier.padding(horizontal = GUTTER, vertical = 8.dp)) {
        steps.forEachIndexed { i, step ->
            Text(
                "${i + 1}.  $step",
                style = MaterialTheme.typography.body,
                color = MaterialTheme.tokens.ink,
                modifier = Modifier.padding(vertical = 3.dp),
            )
        }
    }
}

/** A logged reminder: its task, the day with when it was due (and fired, if late), and how late it was. */
@Composable
private fun HistoryRow(item: HistoryItem, zone: ZoneId) {
    val tokens = MaterialTheme.tokens
    val entry = item.entry
    val due = entry.dueAt.atZone(zone)
    val late = lateness(entry.dueAt, entry.firedAt)
    val day = dayLabel(due.toLocalDate(), LocalDate.now(zone))
    val dayLine = if (late is Lateness.Late) {
        stringResource(
            R.string.health_history_day_due_fired,
            day,
            timeLabel(due.toLocalTime()),
            timeLabel(entry.firedAt.atZone(zone).toLocalTime()),
        )
    } else {
        stringResource(R.string.health_history_day_due, day, timeLabel(due.toLocalTime()))
    }
    Row(
        Modifier.fillMaxWidth().rowDivider(tokens.hairline, GUTTER).padding(horizontal = GUTTER, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                historyTitle(item.title),
                style = MaterialTheme.typography.body,
                color = if (item.title is HistoryTitle.Task) tokens.ink else tokens.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(dayLine, style = MaterialTheme.typography.meta, color = tokens.ink2)
        }
        Text(
            latenessLabel(late),
            style = MaterialTheme.typography.meta,
            fontWeight = if (late is Lateness.Late) FontWeight.Bold else FontWeight.SemiBold,
            color = if (late is Lateness.Late) tokens.danger else tokens.ink2,
        )
    }
}

@Composable
private fun historyTitle(title: HistoryTitle): String = when (title) {
    is HistoryTitle.Task -> title.title
    HistoryTitle.Deleted -> stringResource(R.string.health_deleted_task)
    HistoryTitle.Test -> stringResource(R.string.reminder_test_title)
}

/** "On time", or "3 h 42 min late". */
@Composable
private fun latenessLabel(late: Lateness): String = when (late) {
    Lateness.OnTime -> stringResource(R.string.health_on_time)
    is Lateness.Late -> stringResource(R.string.health_late, focusDuration(late.by))
}

private fun checks(context: Context): List<Check> {
    val uri = Uri.fromParts("package", context.packageName, null)
    val notifications = context.canPostNotifications()
    val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    val notificationSettings = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
    val exactSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, uri)
    } else {
        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
    }
    return listOf(
        Check(CheckKind.BATTERY, isUnrestricted(context)) { openBatterySetting(context) },
        Check(CheckKind.NOTIFICATIONS, notifications) { context.startActivity(notificationSettings) },
        Check(CheckKind.EXACT_ALARMS, exact) { context.startActivity(exactSettings) },
    )
}

/** Battery steps for the phone's maker, with a generic fallback (AGENTS.md, reminder health). */
private fun makerSteps(): Int = when (Build.MANUFACTURER.lowercase()) {
    "samsung" -> R.array.steps_samsung
    "xiaomi", "redmi", "poco" -> R.array.steps_xiaomi
    "oppo", "realme" -> R.array.steps_oppo
    "vivo" -> R.array.steps_vivo
    "oneplus" -> R.array.steps_oneplus
    "huawei", "honor" -> R.array.steps_huawei
    "tecno", "infinix", "itel" -> R.array.steps_transsion
    else -> R.array.steps_generic
}
