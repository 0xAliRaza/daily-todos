package com.aliraza.dailytodos.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.BuildConfig
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.backup.BackupContent
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.HeadsUp
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.ThemeMode
import com.aliraza.dailytodos.core.model.TimerSignal
import com.aliraza.dailytodos.core.model.WeekStart
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.LocalClockFormat
import com.aliraza.dailytodos.ui.components.PillChoice
import com.aliraza.dailytodos.ui.components.phoneFirstDay
import com.aliraza.dailytodos.ui.components.timeLabel
import com.aliraza.dailytodos.ui.focus.signalLabel
import com.aliraza.dailytodos.ui.theme.tokens
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import android.provider.Settings as AndroidSettings

private const val BACKUP_MIME = "application/json"
private val BACKUP_DATE: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val ROW_HEIGHT = 60.dp
private val ROW_PADDING = PaddingValues(horizontal = 18.dp, vertical = 10.dp)

/** Every setting, in five groups: reminders, reliability, focus, general and the user's data. */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    padding: PaddingValues,
    onHealth: () -> Unit,
    onFocusStats: () -> Unit,
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val defaultList by viewModel.defaultListName.collectAsStateWithLifecycle()
    val pending by viewModel.pendingImport.collectAsStateWithLifecycle()
    Messages(viewModel)
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        RemindersGroup(s, defaultList, viewModel)
        ReliabilityGroup(s, onHealth)
        Group(R.string.settings_focus) {
            LinkRow(R.string.stats_title, stringResource(R.string.stats_hint), onClick = onFocusStats)
            ChoiceRow(R.string.settings_signal, R.string.settings_signal_hint, TimerSignal.entries, s.timerSignal, {
                stringResource(signalLabel(it))
            }) { v -> viewModel.update { it.copy(timerSignal = v) } }
        }
        GeneralGroup(s, viewModel)
        DataGroup(s, viewModel)
        Text(
            "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            textAlign = TextAlign.Center,
        )
    }
    pending?.let { ImportConfirm(it, viewModel) }
}

@Composable
private fun RemindersGroup(s: Settings, defaultList: String, viewModel: SettingsViewModel) {
    Group(R.string.settings_reminders) {
        TimeRow(R.string.settings_default_time, R.string.settings_default_time_hint, s.defaultReminderTime) { t ->
            viewModel.update { it.copy(defaultReminderTime = t) }
        }
        SwitchRow(
            stringResource(R.string.settings_date_only),
            R.string.settings_date_only_hint,
            s.dateOnlyGetsReminder,
        ) { on -> viewModel.update { it.copy(dateOnlyGetsReminder = on) } }
        SwitchRow(
            stringResource(R.string.settings_undated_tomorrow, defaultList),
            R.string.settings_undated_tomorrow_hint,
            s.undatedGetsTomorrow,
        ) { on -> viewModel.update { it.copy(undatedGetsTomorrow = on) } }
        ChoiceRow(R.string.settings_heads_up, R.string.settings_heads_up_hint, HeadsUp.entries, s.headsUp, {
            headsUpLabel(it)
        }) { h -> viewModel.update { it.copy(headsUp = h) } }
        SwitchRow(stringResource(R.string.settings_overdue), R.string.settings_overdue_hint, s.overdueReminders) { on ->
            viewModel.update { it.copy(overdueReminders = on) }
        }
        if (s.overdueReminders) {
            TimeRow(R.string.settings_overdue_time, null, s.overdueReminderTime) { t ->
                viewModel.update { it.copy(overdueReminderTime = t) }
            }
        }
        SwitchRow(stringResource(R.string.settings_dnd), R.string.settings_dnd_hint, s.dndOverride) { on ->
            viewModel.update { it.copy(dndOverride = on) }
        }
    }
}

/** Reminder health with the summary of its checks, and the battery setting it depends on most. */
@Composable
private fun ReliabilityGroup(s: Settings, onHealth: () -> Unit) {
    val context = LocalContext.current
    val toFix = rememberHealthChecks(s.dndOverride).count { !it.ok }
    Group(R.string.settings_reliability) {
        LinkRow(
            R.string.health_title,
            if (toFix == 0) {
                stringResource(R.string.settings_health_ok)
            } else {
                pluralStringResource(R.plurals.settings_health_fix, toFix, toFix)
            },
            hintColor = if (toFix == 0) MaterialTheme.tokens.rest else MaterialTheme.tokens.warning,
            onClick = onHealth,
        )
        LinkRow(
            R.string.settings_battery,
            stringResource(
                if (isUnrestricted(context)) R.string.settings_battery_ok else R.string.settings_battery_todo,
            ),
        ) {
            openBatterySetting(context)
        }
    }
}

@Composable
private fun GeneralGroup(s: Settings, viewModel: SettingsViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    Group(R.string.settings_general) {
        ChoiceRow(R.string.settings_theme, null, ThemeMode.entries, s.theme, { stringResource(themeLabel(it)) }) { v ->
            viewModel.update { it.copy(theme = v) }
        }
        ChoiceRow(R.string.settings_clock, null, ClockFormat.entries, s.clockFormat, {
            stringResource(if (it == ClockFormat.H12) R.string.clock_12 else R.string.clock_24)
        }) { v -> viewModel.update { it.copy(clockFormat = v) } }
        ChoiceRow(R.string.settings_week_start, null, WeekStart.entries, s.weekStart, { start ->
            val day = start.resolve(phoneFirstDay()).getDisplayName(TextStyle.FULL, locale)
            if (start == WeekStart.PHONE) stringResource(R.string.week_start_phone, day) else day
        }) { v -> viewModel.update { it.copy(weekStart = v) } }
        SwitchRow(stringResource(R.string.settings_haptics), R.string.settings_haptics_hint, s.haptics) { on ->
            viewModel.update { it.copy(haptics = on) }
        }
    }
}

@Composable
private fun DataGroup(s: Settings, viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri != null) viewModel.export { context.contentResolver.openOutputStream(uri, "wt") }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readImport { context.contentResolver.openInputStream(uri) }
    }
    Group(R.string.settings_data) {
        LinkRow(R.string.backup_export, stringResource(R.string.backup_last, lastBackup(s, viewModel))) {
            exporter.launch(viewModel.backupFileName())
        }
        LinkRow(R.string.backup_import, stringResource(R.string.backup_import_hint)) {
            importer.launch(arrayOf(BACKUP_MIME, "text/plain", "application/octet-stream"))
        }
    }
}

/** When the last backup was made: the date in the phone's style, the time in the app's clock format. */
@Composable
private fun lastBackup(s: Settings, viewModel: SettingsViewModel): String {
    val at = s.lastBackupAt?.atZone(viewModel.zone) ?: return stringResource(R.string.backup_never)
    return "${at.format(BACKUP_DATE)}, ${timeLabel(at.toLocalTime())}"
}

@Composable
private fun ImportConfirm(content: BackupContent, viewModel: SettingsViewModel) {
    val cancel = { viewModel.pendingImport.value = null }
    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(stringResource(R.string.backup_confirm_title)) },
        text = { Text(stringResource(R.string.backup_confirm_body, content.tasks.size, content.lists.size)) },
        confirmButton = {
            TextButton(onClick = viewModel::confirmImport) { Text(stringResource(R.string.backup_confirm_replace)) }
        },
        dismissButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Shows the view model's messages as toasts. A message may name the app as its one argument. */
@Composable
fun Messages(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel, resources) {
        viewModel.message.collect {
            val text = resources.getString(it, resources.getString(R.string.app_name))
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun headsUpLabel(h: HeadsUp): String = when (h) {
    HeadsUp.NONE -> stringResource(R.string.settings_heads_up_none)
    else -> stringResource(R.string.focus_minutes, h.minutes.toInt())
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

fun isUnrestricted(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

/**
 * The battery exemption dialog while restricted; once unrestricted, the app's own settings page.
 * BatteryLife is a Play Store policy check. The app is not distributed there, and a reminder that always fires
 * is the app's whole promise, which is the case the exemption exists for.
 */
@SuppressLint("BatteryLife")
fun openBatterySetting(context: Context) {
    val uri = Uri.fromParts("package", context.packageName, null)
    val intent = if (isUnrestricted(context)) {
        Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
    } else {
        Intent(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, uri)
    }
    context.startActivity(intent)
}

// Building blocks, shared with the health screen

@Composable
fun Group(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 18.dp, top = 22.dp, bottom = 2.dp),
    )
    Column(Modifier.fillMaxWidth(), content = content)
}

/** A row's title and its optional one-line hint; [hintColor] marks a hint that carries a status. */
@Composable
fun RowText(title: String, hint: String?, modifier: Modifier = Modifier, hintColor: Color = Color.Unspecified) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = hintColor.takeOrElse { MaterialTheme.colorScheme.onSurfaceVariant },
            )
        }
    }
}

/** The shape every one-line settings row shares, so the rows line up at one height. */
private fun Modifier.settingsRow(onClick: () -> Unit): Modifier =
    fillMaxWidth().clickable(onClick = onClick).heightIn(min = ROW_HEIGHT).padding(ROW_PADDING)

@Composable
private fun SwitchRow(title: String, hint: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.settingsRow { onChange(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        RowText(title, hint?.let { stringResource(it) }, Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}

/** A row that opens something: a screen, a system setting or a file picker. */
@Composable
fun LinkRow(title: Int, hint: String?, hintColor: Color = Color.Unspecified, onClick: () -> Unit) {
    Row(Modifier.settingsRow(onClick), verticalAlignment = Alignment.CenterVertically) {
        RowText(stringResource(title), hint, Modifier.weight(1f), hintColor)
        Icon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

/** A title, an optional hint and the options under them as one [PillChoice]. */
@Composable
private fun <T> ChoiceRow(
    title: Int,
    hint: Int?,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().heightIn(min = ROW_HEIGHT).padding(ROW_PADDING)) {
        RowText(stringResource(title), hint?.let { stringResource(it) })
        Spacer(Modifier.height(8.dp))
        PillChoice(options, selected, label, onPick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRow(title: Int, hint: Int?, time: LocalTime, onPick: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.settingsRow { open = true }, verticalAlignment = Alignment.CenterVertically) {
        RowText(stringResource(title), hint?.let { stringResource(it) }, Modifier.weight(1f))
        Text(timeLabel(time), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
    }
    if (open) {
        val state = rememberTimePickerState(
            time.hour,
            time.minute,
            is24Hour = LocalClockFormat.current == ClockFormat.H24,
        )
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    onPick(LocalTime.of(state.hour, state.minute))
                    open = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state) },
        )
    }
}
