package com.aliraza.dailytodos.ui.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.model.ReminderLogEntry
import com.aliraza.dailytodos.core.reminder.Lateness
import com.aliraza.dailytodos.core.reminder.lateness
import com.aliraza.dailytodos.reminder.canPostNotifications
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.dayLabel
import com.aliraza.dailytodos.ui.components.timeLabel
import com.aliraza.dailytodos.ui.focus.focusDuration
import com.aliraza.dailytodos.ui.theme.tokens
import java.time.LocalDate
import java.time.ZoneId
import android.provider.Settings as AndroidSettings

/** The reminder checks, run again whenever the app comes back to the front. Settings shows their summary. */
@Composable
internal fun rememberHealthChecks(dndOverride: Boolean): List<Check> {
    val context = LocalContext.current
    var resumed by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumed++ }
    return remember(resumed, dndOverride) { checks(context) }
}

internal data class Check(val title: Int, val ok: Boolean, val okHint: Int, val fixHint: Int, val fix: (() -> Unit)?)

/** Every permission and battery setting reminders depend on, the maker's steps, a test and the history. */
@Composable
fun HealthScreen(viewModel: SettingsViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    Messages(viewModel)
    val checks = rememberHealthChecks(settings.dndOverride)
    val appName = stringResource(R.string.app_name)
    val testTitle = stringResource(R.string.health_test_title, appName)
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        StatusBanner(checks.count { !it.ok })
        Group(R.string.health_checks) { checks.forEach { CheckRow(it) } }
        Group(R.string.health_maker_steps) {
            val steps = stringArrayResource(makerSteps()).map { it.format(appName) }
            Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                steps.forEachIndexed { i, step ->
                    Text(
                        "${i + 1}.  $step",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
            }
        }
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(
                    MaterialTheme.colorScheme.primary,
                ).clickable(role = Role.Button) { viewModel.sendTestReminder(testTitle) }
                .padding(vertical = 16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Bell, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.health_test),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Group(R.string.health_history) {
            if (history.isEmpty()) {
                Text(
                    stringResource(R.string.health_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(18.dp),
                )
            }
            history.forEach { HistoryRow(it, viewModel.zone) }
        }
        Text(
            stringResource(R.string.health_history_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 10.dp),
        )
    }
}

/** "All set" with a green tick, or how many checks need a tap, with an amber mark. */
@Composable
private fun StatusBanner(toFix: Int) {
    val text = if (toFix == 0) {
        stringResource(R.string.health_all_set)
    } else {
        pluralStringResource(R.plurals.health_to_fix, toFix, toFix)
    }
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(ok = toFix == 0)
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun CheckRow(check: Check) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clickable(enabled = check.fix != null) { check.fix?.invoke() }
            .heightIn(min = 60.dp).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(check.ok)
        Spacer(Modifier.width(14.dp))
        RowText(
            stringResource(check.title),
            stringResource(if (check.ok) check.okHint else check.fixHint),
            Modifier.weight(1f),
        )
        if (check.fix != null) Icon(AppIcons.ChevronRight, null, tint = scheme.outline)
    }
}

/** A green tick for a pass, an amber mark for something to fix, each on a faint ring of its colour. */
@Composable
private fun StatusDot(ok: Boolean) {
    val ink = if (ok) MaterialTheme.tokens.rest else MaterialTheme.tokens.warning
    Box(
        Modifier.size(28.dp).clip(CircleShape).background(ink.copy(alpha = DOT_TINT)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (ok) AppIcons.Check else AppIcons.Alert, null, tint = ink, modifier = Modifier.size(18.dp))
    }
}

private const val DOT_TINT = 0.14f

@Composable
private fun HistoryRow(entry: ReminderLogEntry, zone: ZoneId) {
    val due = entry.dueAt.atZone(zone)
    val fired = entry.firedAt.atZone(zone)
    val late = lateness(entry.dueAt, entry.firedAt)
    val scheme = MaterialTheme.colorScheme
    val lateInk = if (late is Lateness.Late) MaterialTheme.tokens.warning else scheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(
            dayLabel(due.toLocalDate(), LocalDate.now(zone)),
            stringResource(R.string.health_history_due, timeLabel(due.toLocalTime()), timeLabel(fired.toLocalTime())),
            Modifier.weight(1f),
        )
        Text(
            latenessLabel(late),
            style = MaterialTheme.typography.labelMedium,
            color = lateInk,
        )
    }
}

/** "On time", or "3 h 42 min late". */
@Composable
private fun latenessLabel(late: Lateness): String = when (late) {
    Lateness.OnTime -> stringResource(R.string.health_on_time)
    is Lateness.Late -> stringResource(R.string.health_late, focusDuration(late.by))
}

private fun checks(context: Context): List<Check> {
    val uri = Uri.fromParts("package", context.packageName, null)
    val open = { intent: Intent -> { context.startActivity(intent) } }
    val notifications = context.canPostNotifications()
    val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    val battery = isUnrestricted(context)
    return listOf(
        Check(
            R.string.health_notifications,
            notifications,
            R.string.health_allowed,
            R.string.health_notifications_fix,
            open(
                Intent(
                    AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS,
                ).putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName),
            )
                .takeIf { !notifications },
        ),
        Check(
            R.string.health_exact,
            exact,
            R.string.health_exact_ok,
            R.string.health_exact_fix,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !exact) {
                open(
                    Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, uri),
                )
            } else {
                null
            },
        ),
        Check(
            R.string.settings_battery,
            battery,
            R.string.settings_battery_ok,
            R.string.settings_battery_todo,
        ) { openBatterySetting(context) },
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
