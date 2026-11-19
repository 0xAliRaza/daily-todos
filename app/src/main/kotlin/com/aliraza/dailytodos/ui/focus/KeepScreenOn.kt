package com.aliraza.dailytodos.ui.focus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.aliraza.dailytodos.R
import com.aliraza.dailytodos.ui.components.AppIcons
import com.aliraza.dailytodos.ui.components.rememberTick
import com.aliraza.dailytodos.ui.theme.tokens

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

/** Touch target of the toggle; the cup sits in its middle. */
private val TOGGLE_SIZE = 40.dp

/**
 * The timer screen's switch for the "Keep screen on" setting: a cup, filled in the accent while on.
 * Screen readers hear it as a switch with its state.
 */
@Composable
internal fun KeepScreenOnToggle(on: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val tick = rememberTick()
    Box(
        modifier.size(TOGGLE_SIZE).clip(CircleShape).toggleable(value = on, role = Role.Switch) {
            tick()
            onChange(it)
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (on) AppIcons.CupFilled else AppIcons.Cup,
            stringResource(R.string.keep_screen_on),
            tint = if (on) MaterialTheme.tokens.accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
