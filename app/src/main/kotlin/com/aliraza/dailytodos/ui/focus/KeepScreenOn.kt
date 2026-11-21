package com.aliraza.dailytodos.ui.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.ui.components.AppChip
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.PillTone
import com.aliraza.dailytodos.ui.components.rememberTick

/**
 * Keeps the app's window awake while [on]: the window holds the screen on for as long as the view is
 * shown, so it works on every screen and ends when the app goes to the background. No wake lock.
 */
@Composable
fun KeepScreenOn(on: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, on) {
        view.keepScreenOn = on
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * The timer screen's switch for the Caffeinated setting: a labelled "Screen on" chip with the cup, filled
 * while on. Screen readers hear it as a switch with its state.
 */
@Composable
internal fun KeepScreenOnToggle(
    on: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    tone: PillTone = PillTone.ACCENT,
) {
    val tick = rememberTick()
    val label = stringResource(R.string.focus_screen_on)
    val flip = {
        tick()
        onChange(!on)
    }
    AppChip(
        label,
        onClick = flip,
        modifier = modifier.clearAndSetSemantics {
            role = Role.Switch
            contentDescription = label
            toggleableState = ToggleableState(on)
            onClick {
                flip()
                true
            }
        },
        icon = if (on) AppIcons.CupFilled else AppIcons.Cup,
        set = on,
        tone = tone,
    )
}
