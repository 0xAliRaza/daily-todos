package com.aliraza.dailytodos.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every text and icon pairing in both themes meets WCAG AA (4.5:1 for text, 3:1 for icons and fills). */
class ThemeContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()) + 0.05
        val darker = minOf(a.luminance(), b.luminance()) + 0.05
        return lighter / darker
    }

    private fun textPairs(t: ColorTokens) = mapOf(
        "onBackground on background" to (t.onBackground to t.background),
        "onBackground on surface" to (t.onBackground to t.surface),
        "onBackground on surfaceVariant" to (t.onBackground to t.surfaceVariant),
        "onSurfaceVariant on background" to (t.onSurfaceVariant to t.background),
        "onSurfaceVariant on surface" to (t.onSurfaceVariant to t.surface),
        "onSurfaceVariant on surfaceVariant" to (t.onSurfaceVariant to t.surfaceVariant),
        "accentText on background" to (t.accentText to t.background),
        "accentText on surface" to (t.accentText to t.surface),
        "onAccent on accent" to (t.onAccent to t.accent),
        "onAccentContainer on accentContainer" to (t.onAccentContainer to t.accentContainer),
        "onError on error" to (t.onError to t.error),
        "error on background" to (t.error to t.background),
        "overdue on background" to (t.overdue to t.background),
        "overdue on surface" to (t.overdue to t.surface),
        "accentText on accentContainer" to (t.accentText to t.accentContainer),
        "onSurfaceVariant on accentContainer" to (t.onSurfaceVariant to t.accentContainer),
    )

    private fun graphicPairs(t: ColorTokens) = mapOf(
        "accent on background" to (t.accent to t.background),
        "rest on background" to (t.rest to t.background),
        "accent on surface" to (t.accent to t.surface),
        "outline on background" to (t.outline to t.background),
        "outline on surface" to (t.outline to t.surface),
        "priority1 on background" to (t.priority1 to t.background),
        "priority2 on background" to (t.priority2 to t.background),
        "priority3 on background" to (t.priority3 to t.background),
        "priority1 on surface" to (t.priority1 to t.surface),
        "priority2 on surface" to (t.priority2 to t.surface),
        "priority3 on surface" to (t.priority3 to t.surface),
    )

    private fun assertAll(name: String, pairs: Map<String, Pair<Color, Color>>, minimum: Double) {
        pairs.forEach { (label, pair) ->
            val actual = ratio(pair.first, pair.second)
            assertTrue("$name: $label is ${"%.2f".format(actual)}:1, needs $minimum:1", actual >= minimum)
        }
    }

    @Test
    fun lightThemeMeetsAa() {
        assertAll("light", textPairs(LightTokens), TEXT_MINIMUM)
        assertAll("light", graphicPairs(LightTokens), GRAPHIC_MINIMUM)
    }

    @Test
    fun darkThemeMeetsAa() {
        assertAll("dark", textPairs(DarkTokens), TEXT_MINIMUM)
        assertAll("dark", graphicPairs(DarkTokens), GRAPHIC_MINIMUM)
    }

    private companion object {
        const val TEXT_MINIMUM = 4.5
        const val GRAPHIC_MINIMUM = 3.0
    }
}
