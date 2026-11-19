package com.aliraza.dailytasks.ui.settings

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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytasks.BuildConfig
import com.aliraza.dailytasks.R
import com.aliraza.dailytasks.core.backup.BackupContent
import com.aliraza.dailytasks.core.model.HeadsUp
import com.aliraza.dailytasks.core.model.Settings
import com.aliraza.dailytasks.core.model.ThemeMode
import com.aliraza.dailytasks.core.model.TimerSignal
import com.aliraza.dailytasks.ui.components.AppIcons
import com.aliraza.dailytasks.ui.components.timeLabel
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import android.provider.Settings as AndroidSettings

private const val BACKUP_MIME = "application/json"
private val BACKUP_TIME: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(
    FormatStyle.MEDIUM,
    FormatStyle.SHORT,
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    padding: PaddingValues,
    onHealth: () -> Unit,
    onFocusStats: () -> Unit,
) {
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val pending by viewModel.pendingImport.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Messages(viewModel)
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        if (uri != null) viewModel.export { context.contentResolver.openOutputStream(uri, "wt") }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readImport { context.contentResolver.openInputStream(uri) }
    }
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        RemindersGroup(s, viewModel)
        Group(R.string.settings_reliability) {
            LinkRow(R.string.health_title, stringResource(R.string.settings_health_hint), onHealth)
            LinkRow(
                R.string.settings_battery,
                stringResource(
                    if (isUnrestricted(context)) R.string.settings_battery_ok else R.string.settings_battery_todo,
                ),
            ) {
                openBatterySetting(context)
            }
        }
        Group(R.string.settings_focus) {
            LinkRow(R.string.stats_title, stringResource(R.string.stats_hint), onFocusStats)
            ChoiceRow(
                R.string.settings_signal,
                TimerSignal.entries,
                s.timerSignal,
                label = { stringResource(signalLabel(it)) },
            ) { v -> viewModel.update { it.copy(timerSignal = v) } }
        }
        Group(R.string.settings_appearance) {
            ChoiceRow(
                R.string.settings_theme,
                ThemeMode.entries,
                s.theme,
                label = { stringResource(themeLabel(it)) },
            ) { v ->
                viewModel.update { it.copy(theme = v) }
            }
        }
        Group(R.string.settings_data) {
            LinkRow(R.string.backup_export, stringResource(R.string.backup_last, lastBackup(s, viewModel))) {
                exporter.launch(viewModel.backupFileName())
            }
            LinkRow(R.string.backup_import, stringResource(R.string.backup_import_hint)) {
                importer.launch(arrayOf(BACKUP_MIME, "text/plain", "application/octet-stream"))
            }
        }
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
private fun lastBackup(s: Settings, viewModel: SettingsViewModel): String =
    s.lastBackupAt?.atZone(viewModel.zone)?.format(BACKUP_TIME) ?: stringResource(R.string.backup_never)

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

@Composable
private fun RemindersGroup(s: Settings, viewModel: SettingsViewModel) {
    Group(R.string.settings_reminders) {
        TimeRow(R.string.settings_default_time, R.string.settings_default_time_hint, s.defaultReminderTime) { t ->
            viewModel.update { it.copy(defaultReminderTime = t) }
        }
        SwitchRow(
            R.string.settings_undated_tomorrow,
            R.string.settings_undated_tomorrow_hint,
            s.undatedGetsTomorrow,
        ) { on ->
            viewModel.update { it.copy(undatedGetsTomorrow = on) }
        }
        SwitchRow(R.string.settings_date_only, R.string.settings_date_only_hint, s.dateOnlyGetsReminder) { on ->
            viewModel.update { it.copy(dateOnlyGetsReminder = on) }
        }
        ChoiceRow(
            R.string.settings_heads_up,
            HeadsUp.entries,
            s.headsUp,
            label = {
                if (it == HeadsUp.NONE) {
                    stringResource(
                        R.string.settings_heads_up_none,
                    )
                } else {
                    stringResource(R.string.focus_minutes, it.minutes.toInt())
                }
            },
        ) { h -> viewModel.update { it.copy(headsUp = h) } }
        SwitchRow(R.string.settings_overdue, R.string.settings_overdue_hint, s.overdueReminders) { on ->
            viewModel.update { it.copy(overdueReminders = on) }
        }
        if (s.overdueReminders) {
            TimeRow(
                R.string.settings_overdue_time,
                null,
                s.overdueReminderTime,
            ) { t -> viewModel.update { it.copy(overdueReminderTime = t) } }
        }
        SwitchRow(
            R.string.settings_dnd,
            R.string.settings_dnd_hint,
            s.dndOverride,
        ) { on -> viewModel.update { it.copy(dndOverride = on) } }
    }
}

@Composable
fun Messages(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) { viewModel.message.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() } }
}

@Composable
private fun headsUpLabel(h: HeadsUp): String = when (h) {
    HeadsUp.NONE -> stringResource(R.string.settings_heads_up_none)
    else -> stringResource(R.string.focus_minutes, h.minutes.toInt())
}

private fun signalLabel(signal: TimerSignal) = when (signal) {
    TimerSignal.SOUND -> R.string.signal_sound
    TimerSignal.VIBRATE -> R.string.signal_vibrate
    TimerSignal.BOTH -> R.string.signal_both
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
 * BatteryLife is a Play Store policy check. DT is not distributed there, and a reminder that always fires
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

@Composable
fun RowText(title: String, hint: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SwitchRow(title: Int, hint: Int?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable {
            onChange(
                !checked,
            )
        }.heightIn(min = 60.dp).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(stringResource(title), hint?.let { stringResource(it) }, Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
fun LinkRow(title: Int, hint: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(
            onClick = onClick,
        ).heightIn(min = 60.dp).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(stringResource(title), hint, Modifier.weight(1f))
        Icon(AppIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
    title: Int,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onPick(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    icon = {},
                ) { Text(label(option), style = MaterialTheme.typography.labelMedium, maxLines = 1) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeRow(title: Int, hint: Int?, time: LocalTime, onPick: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { open = true }.heightIn(
            min = 60.dp,
        ).padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(stringResource(title), hint?.let { stringResource(it) }, Modifier.weight(1f))
        Text(timeLabel(time), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
    }
    if (open) {
        val state = rememberTimePickerState(time.hour, time.minute)
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
