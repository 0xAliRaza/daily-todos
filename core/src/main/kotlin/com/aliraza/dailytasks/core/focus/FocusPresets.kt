package com.aliraza.dailytasks.core.focus

/**
 * The quick lengths on the timer screen, in minutes: three fixed presets, then a fourth chip that holds
 * the last length picked by hand that is not one of them.
 */
object FocusPresets {
    val FOCUS = listOf(15, 25, 50)
    val BREAK = listOf(5, 10, 15)

    /** The fourth chip once [picked] is chosen: a preset leaves [custom] as it was, any other length replaces it. */
    fun customAfter(presets: List<Int>, custom: Int, picked: Int): Int = if (picked in presets) custom else picked
}
