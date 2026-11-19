package com.aliraza.dailytodos.core.focus

/**
 * Whether the app keeps the screen awake: only with the setting [enabled] and while a focus round or a
 * break counts down. A paused round, a waiting break and an idle timer let the phone sleep as usual.
 */
fun keepsScreenOn(enabled: Boolean, timer: FocusTimer): Boolean = enabled && timer.isRunning
