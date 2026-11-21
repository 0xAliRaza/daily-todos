package com.aliraza.dailytodos.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliraza.dailytodos.BuildConfig
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.core.backup.BackupContent
import com.aliraza.dailytodos.core.model.Accent
import com.aliraza.dailytodos.core.model.ClockFormat
import com.aliraza.dailytodos.core.model.HeadsUp
import com.aliraza.dailytodos.core.model.Settings
import com.aliraza.dailytodos.core.model.ThemeMode
import com.aliraza.dailytodos.core.model.TimerSignal
import com.aliraza.dailytodos.core.model.WeekStart
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.PillChoice
import com.aliraza.dailytodos.ui.components.TimeDialog
import com.aliraza.dailytodos.ui.components.phoneFirstDay
import com.aliraza.dailytodos.ui.components.rowDivider
import com.aliraza.dailytodos.ui.components.timeLabel
import com.aliraza.dailytodos.ui.focus.signalLabel
import com.aliraza.dailytodos.ui.theme.body
import com.aliraza.dailytodos.ui.theme.groupLabel
import com.aliraza.dailytodos.ui.theme.meta
import com.aliraza.dailytodos.ui.theme.taskTitle
import com.aliraza.dailytodos.ui.theme.tokens
import com.aliraza.dailytodos.ui.theme.tokensFor
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import android.provider.Settings as AndroidSettings

private const val BACKUP_MIME = "application/json"
private val BACKUP_DATE: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** The side gutter of settings and Reminder health; row dividers start at it. */
internal val GUTTER = 20.dp

/**
 * Every setting in groups: reminders, reliability, focus, appearance, general and the user's data. When a
 * reminder check fails, a banner above them all says reminders may be late and opens Reminder health.
 */
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
    val failing = rememberHealthChecks().filterNot { it.ok }
    Messages(viewModel)
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        failing.firstOrNull()?.let { LateBanner(it, failing.size, onHealth) }
        RemindersGroup(s, defaultList, viewModel)
        ReliabilityGroup(failing.size, onHealth)
        FocusGroup(s, viewModel, onFocusStats)
        AppearanceGroup(s, viewModel)
        GeneralGroup(s, viewModel)
        DataGroup(s, viewModel)
        Text(
            "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.meta,
            color = MaterialTheme.tokens.ink2,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            textAlign = TextAlign.Center,
        )
    }
    pending?.let { ImportConfirm(it, viewModel) }
}

/** "Reminders may be late", what the first failing check means and how many there are; opens Reminder health. */
@Composable
private fun LateBanner(first: Check, count: Int, onHealth: () -> Unit) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.padding(start = GUTTER, end = GUTTER, top = 10.dp, bottom = 4.dp).fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(tokens.dangerContainer)
            .clickable(role = Role.Button, onClick = onHealth)
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(AppIcons.Alert, null, tint = tokens.danger, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.settings_late_title),
                style = MaterialTheme.typography.taskTitle,
                fontWeight = FontWeight.Bold,
                color = tokens.ink,
            )
            Text(
                stringResource(
                    R.string.settings_late_detail,
                    stringResource(problemOf(first)),
                    pluralStringResource(R.plurals.health_to_fix, count, count),
                ),
                style = MaterialTheme.typography.meta,
                color = tokens.ink2,
            )
        }
        Icon(AppIcons.ChevronRight, null, tint = tokens.ink2, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RemindersGroup(s: Settings, defaultList: String, viewModel: SettingsViewModel) {
    Group(R.string.settings_reminders) {
        TimeRow(stringResource(R.string.settings_default_time), s.defaultReminderTime) { t ->
            viewModel.update { it.copy(defaultReminderTime = t) }
        }
        SwitchRow(
            stringResource(R.string.settings_date_only),
            stringResource(R.string.settings_date_only_hint),
            s.dateOnlyGetsReminder,
        ) { on -> viewModel.update { it.copy(dateOnlyGetsReminder = on) } }
        SwitchRow(
            stringResource(R.string.settings_undated_tomorrow, defaultList),
            stringResource(R.string.settings_undated_tomorrow_hint),
            s.undatedGetsTomorrow,
        ) { on -> viewModel.update { it.copy(undatedGetsTomorrow = on) } }
        ChoiceRow(
            stringResource(R.string.settings_heads_up),
            stringResource(R.string.settings_heads_up_hint),
            HeadsUp.entries,
            s.headsUp,
            { headsUpLabel(it) },
        ) { h -> viewModel.update { it.copy(headsUp = h) } }
        SwitchRow(
            stringResource(R.string.settings_overdue),
            stringResource(R.string.settings_overdue_hint),
            s.overdueReminders,
        ) { on -> viewModel.update { it.copy(overdueReminders = on) } }
        if (s.overdueReminders) {
            TimeRow(stringResource(R.string.settings_overdue_time), s.overdueReminderTime, indented = true) { t ->
                viewModel.update { it.copy(overdueReminderTime = t) }
            }
        }
        SwitchRow(
            stringResource(R.string.settings_dnd),
            stringResource(R.string.settings_dnd_hint),
            s.dndOverride,
        ) { on -> viewModel.update { it.copy(dndOverride = on) } }
    }
}

/** Reminder health with the summary of its checks, and the battery setting it depends on most. */
@Composable
private fun ReliabilityGroup(toFix: Int, onHealth: () -> Unit) {
    val context = LocalContext.current
    val tokens = MaterialTheme.tokens
    Group(R.string.settings_reliability) {
        ValueRow(
            stringResource(R.string.health_title),
            value = if (toFix == 0) {
                stringResource(R.string.settings_health_ok)
            } else {
                pluralStringResource(R.plurals.settings_health_fix, toFix, toFix)
            },
            valueColor = if (toFix == 0) tokens.rest else tokens.danger,
            onClick = onHealth,
        )
        ValueRow(
            stringResource(R.string.settings_battery),
            value = stringResource(
                if (isUnrestricted(context)) R.string.settings_battery_ok else R.string.health_value_restricted,
            ),
        ) { openBatterySetting(context) }
    }
}

@Composable
private fun FocusGroup(s: Settings, viewModel: SettingsViewModel, onFocusStats: () -> Unit) {
    Group(R.string.settings_focus) {
        ValueRow(
            stringResource(R.string.stats_title),
            hint = stringResource(R.string.stats_hint),
            onClick = onFocusStats,
        )
        ChoiceRow(
            stringResource(R.string.settings_signal),
            stringResource(R.string.settings_signal_hint),
            TimerSignal.entries,
            s.timerSignal,
            { stringResource(signalLabel(it)) },
        ) { v -> viewModel.update { it.copy(timerSignal = v) } }
        SwitchRow(
            stringResource(R.string.keep_screen_on),
            stringResource(R.string.settings_keep_screen_on_hint),
            s.keepScreenOn,
        ) { on -> viewModel.update { it.copy(keepScreenOn = on) } }
    }
}

/** Theme, the black background (only meaningful while dark shows) and the accent. */
@Composable
private fun AppearanceGroup(s: Settings, viewModel: SettingsViewModel) {
    val dark = showsDark(s.theme, isSystemInDarkTheme())
    Group(R.string.settings_appearance) {
        PillChoice(
            ThemeMode.entries,
            s.theme,
            { stringResource(themeLabel(it)) },
            { v -> viewModel.update { it.copy(theme = v) } },
            Modifier.padding(horizontal = GUTTER, vertical = 4.dp),
        )
        SwitchRow(
            stringResource(R.string.settings_black),
            stringResource(R.string.settings_black_hint),
            s.blackBackground,
            enabled = dark,
        ) { on -> viewModel.update { it.copy(blackBackground = on) } }
        AccentRow(s.accent, dark) { a -> viewModel.update { it.copy(accent = a) } }
    }
}

/** Whether the dark theme shows for [theme], given whether the phone is in dark mode now. */
internal fun showsDark(theme: ThemeMode, systemDark: Boolean): Boolean = when (theme) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

private val SWATCH = 32.dp
private val SWATCH_RING = 40.dp
private val SWATCH_TARGET = 44.dp

/** "Accent" and the four accents as swatches in this mode's colours, a check and a ring on the picked one. */
@Composable
private fun AccentRow(picked: Accent, dark: Boolean, onPick: (Accent) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = GUTTER, end = GUTTER - (SWATCH_TARGET - SWATCH) / 2)
            .heightIn(min = 56.dp).selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.settings_accent),
            style = MaterialTheme.typography.body,
            color = MaterialTheme.tokens.ink,
            modifier = Modifier.weight(1f),
        )
        Accent.entries.forEach { accent -> Swatch(accent, accent == picked, dark) { onPick(accent) } }
    }
}

@Composable
private fun Swatch(accent: Accent, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val colors = tokensFor(dark, accent = accent)
    val name = stringResource(accentLabel(accent))
    Box(
        Modifier.size(SWATCH_TARGET).clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(SWATCH_RING).border(2.dp, colors.accent, CircleShape))
        Box(Modifier.size(SWATCH).clip(CircleShape).background(colors.accent), contentAlignment = Alignment.Center) {
            if (selected) Icon(AppIcons.Check, null, tint = colors.onAccent, modifier = Modifier.size(16.dp))
        }
    }
}

internal fun accentLabel(accent: Accent): Int = when (accent) {
    Accent.INK_BLUE -> R.string.accent_ink_blue
    Accent.VIOLET -> R.string.accent_violet
    Accent.TEAL -> R.string.accent_teal
    Accent.GRAPHITE -> R.string.accent_graphite
}

@Composable
private fun GeneralGroup(s: Settings, viewModel: SettingsViewModel) {
    val locale = LocalConfiguration.current.locales[0]
    val phoneDay = phoneFirstDay().getDisplayName(TextStyle.FULL, locale)
    Group(R.string.settings_general) {
        ChoiceRow(stringResource(R.string.settings_clock), null, ClockFormat.entries, s.clockFormat, {
            stringResource(if (it == ClockFormat.H12) R.string.clock_12 else R.string.clock_24)
        }) { v -> viewModel.update { it.copy(clockFormat = v) } }
        ChoiceRow(
            stringResource(R.string.settings_week_start),
            stringResource(R.string.week_start_phone_hint, phoneDay),
            WeekStart.entries,
            s.weekStart,
            { start -> weekStartLabel(start, locale) },
        ) { v -> viewModel.update { it.copy(weekStart = v) } }
        SwitchRow(
            stringResource(R.string.settings_haptics),
            stringResource(R.string.settings_haptics_hint),
            s.haptics,
        ) { on -> viewModel.update { it.copy(haptics = on) } }
    }
}

/** "Phone", "Monday" or "Sunday": the phone's own day is in the hint line, so no day shows twice. */
@Composable
private fun weekStartLabel(start: WeekStart, locale: Locale): String = when (start) {
    WeekStart.PHONE -> stringResource(R.string.week_start_phone_short)
    WeekStart.MONDAY -> DayOfWeek.MONDAY.getDisplayName(TextStyle.FULL, locale)
    WeekStart.SUNDAY -> DayOfWeek.SUNDAY.getDisplayName(TextStyle.FULL, locale)
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
        ValueRow(
            stringResource(R.string.backup_export),
            hint = stringResource(R.string.backup_last, lastBackup(s, viewModel)),
        ) { exporter.launch(viewModel.backupFileName()) }
        ValueRow(stringResource(R.string.backup_import), hint = stringResource(R.string.backup_import_hint)) {
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

/** A group: its label in small capitals, then its rows. */
@Composable
fun Group(title: Int, content: @Composable ColumnScope.() -> Unit) {
    GroupLabel(title)
    Column(Modifier.fillMaxWidth(), content = content)
}

/** A group's label: 12 sp bold capitals in the secondary ink, at the gutter. */
@Composable
internal fun GroupLabel(title: Int) {
    Text(
        stringResource(title).uppercase(LocalConfiguration.current.locales[0]),
        style = MaterialTheme.typography.groupLabel,
        color = MaterialTheme.tokens.ink2,
        modifier = Modifier.padding(start = GUTTER, end = GUTTER, top = 16.dp, bottom = 6.dp),
    )
}

private val ROW_HEIGHT = 56.dp
private val HINTED_ROW_HEIGHT = 64.dp

/** Where a dependent row's text starts, and the line that ties it to the switch above. */
private val INDENT = 44.dp
private val INDENT_LINE = 28.dp
private const val DISABLED_ALPHA = 0.5f

/** A row's title and its optional one-line hint. */
@Composable
private fun RowText(title: String, hint: String?, modifier: Modifier = Modifier) {
    val tokens = MaterialTheme.tokens
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.body, color = tokens.ink)
        if (hint != null) {
            Text(
                hint,
                style = MaterialTheme.typography.meta,
                color = tokens.ink2,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * The shape every settings row shares: a hairline in [line] from the gutter, the height for one or two
 * lines, and for an [indented] row a deeper start with a short line up to the switch it depends on.
 */
private fun Modifier.settingsRow(line: Color, hinted: Boolean, indented: Boolean = false): Modifier = fillMaxWidth()
    .rowDivider(line, GUTTER)
    .then(if (indented) Modifier.drawBehind { dependentLine(line) } else Modifier)
    .heightIn(min = if (hinted) HINTED_ROW_HEIGHT else ROW_HEIGHT)

private fun DrawScope.dependentLine(color: Color) {
    val x = INDENT_LINE.toPx()
    drawLine(color, Offset(x, 0f), Offset(x, size.height / 2), strokeWidth = 2.dp.toPx())
}

private fun rowPadding(indented: Boolean) = PaddingValues(
    start = if (indented) INDENT else GUTTER,
    end = 12.dp,
    top = 8.dp,
    bottom = 8.dp,
)

/** A row that is a switch as a whole; greyed and untappable while not [enabled]. */
@Composable
private fun SwitchRow(
    title: String,
    hint: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.settingsRow(tokens.hairline, hint != null)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(rowPadding(indented = false)).padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(title, hint, Modifier.weight(1f).padding(end = 12.dp))
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = tokens.accent,
                checkedThumbColor = tokens.onAccent,
                uncheckedTrackColor = tokens.fill,
                uncheckedThumbColor = tokens.ink3,
                uncheckedBorderColor = tokens.ink3,
                disabledUncheckedTrackColor = tokens.fill,
                disabledUncheckedThumbColor = tokens.ink3,
                disabledUncheckedBorderColor = tokens.ink3,
                disabledCheckedTrackColor = tokens.accent,
                disabledCheckedThumbColor = tokens.onAccent,
            ),
        )
    }
}

/** A row that opens something: a screen, a dialog, a system setting or a file picker. It ends in value + chevron. */
@Composable
private fun ValueRow(
    title: String,
    hint: String? = null,
    value: String? = null,
    valueColor: Color = Color.Unspecified,
    indented: Boolean = false,
    onClick: () -> Unit,
) {
    val tokens = MaterialTheme.tokens
    Row(
        Modifier.settingsRow(tokens.hairline, hint != null, indented).clickable(role = Role.Button, onClick = onClick)
            .padding(rowPadding(indented)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowText(title, hint, Modifier.weight(1f))
        value?.let {
            Text(
                it,
                style = MaterialTheme.typography.body,
                fontWeight = FontWeight.Medium,
                color = valueColor.takeOrElse { tokens.ink2 },
            )
        }
        Icon(AppIcons.ChevronRight, null, tint = tokens.ink3, modifier = Modifier.size(18.dp))
    }
}

/** A title, an optional hint and the options under them as one [PillChoice]. */
@Composable
private fun <T> ChoiceRow(
    title: String,
    hint: String?,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
) {
    Column(
        Modifier.settingsRow(MaterialTheme.tokens.hairline, hinted = false)
            .padding(start = GUTTER, end = GUTTER, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RowText(title, hint)
        PillChoice(options, selected, label, onPick)
    }
}

/** A time setting: its value in the app's clock format, opening the time dialog. */
@Composable
private fun TimeRow(title: String, time: LocalTime, indented: Boolean = false, onPick: (LocalTime) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ValueRow(title, value = timeLabel(time), indented = indented) { open = true }
    if (open) TimeDialog(time, onDismiss = { open = false }, onPick = onPick)
}
