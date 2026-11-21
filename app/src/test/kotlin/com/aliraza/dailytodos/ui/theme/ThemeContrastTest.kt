package com.aliraza.dailytodos.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.aliraza.dailytodos.core.model.Accent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every text and graphic pairing meets WCAG AA (4.5:1 for text, 3:1 for rings, dots and fills that carry
 * meaning) for every accent in light, dark and dark on black.
 */
class ThemeContrastTest {

    private fun ratio(a: Color, b: Color): Double {
        val lighter = maxOf(a.luminance(), b.luminance()) + 0.05
        val darker = minOf(a.luminance(), b.luminance()) + 0.05
        return lighter / darker
    }

    /** The surfaces content sits on: the page, a sheet and the fill of pills, chips and fields. */
    private fun surfaces(t: ColorTokens) = mapOf("background" to t.background, "sheet" to t.sheet, "fill" to t.fill)

    private fun textPairs(t: ColorTokens): Map<String, Pair<Color, Color>> {
        val onSurfaces = listOf("ink" to t.ink, "ink2" to t.ink2, "accent" to t.accent, "danger" to t.danger)
            .flatMap { (fg, c) -> surfaces(t).map { (bg, s) -> "$fg on $bg" to (c to s) } }
        val onPage = listOf("rest" to t.rest, "warning" to t.warning)
            .flatMap { (fg, c) -> listOf("$fg on background" to (c to t.background), "$fg on sheet" to (c to t.sheet)) }
        return (onSurfaces + onPage).toMap() + mapOf(
            "onAccent on accent" to (t.onAccent to t.accent),
            "onAccentContainer on accentContainer" to (t.onAccentContainer to t.accentContainer),
            "ink on accentContainer" to (t.ink to t.accentContainer),
            "ink2 on accentContainer" to (t.ink2 to t.accentContainer),
            "onAccentContainer on highlight" to (t.onAccentContainer to t.highlight),
            "onInverse on inverse" to (t.onInverse to t.inverse),
            "inverseAction on inverse" to (t.inverseAction to t.inverse),
            "onDanger on danger" to (t.onDanger to t.danger),
            "ink on dangerContainer" to (t.ink to t.dangerContainer),
            "ink2 on dangerContainer" to (t.ink2 to t.dangerContainer),
            "danger on dangerContainer" to (t.danger to t.dangerContainer),
            "onRest on rest" to (t.onRest to t.rest),
            "onRestContainer on restContainer" to (t.onRestContainer to t.restContainer),
        )
    }

    private fun graphicPairs(t: ColorTokens): Map<String, Pair<Color, Color>> {
        val marks = listOf(
            "accent" to t.accent,
            "ink3" to t.ink3,
            "priority1" to t.priority1,
            "priority2" to t.priority2,
            "priority3" to t.priority3,
            "rest" to t.rest,
        )
        val onPageAndSheet = marks.flatMap { (fg, c) ->
            listOf("$fg on background" to (c to t.background), "$fg on sheet" to (c to t.sheet))
        }
        return onPageAndSheet.toMap() + mapOf(
            "accent on fill" to (t.accent to t.fill),
            "rest on restContainer" to (t.rest to t.restContainer),
        )
    }

    private fun assertAll(name: String, pairs: Map<String, Pair<Color, Color>>, minimum: Double) {
        pairs.forEach { (label, pair) ->
            val actual = ratio(pair.first, pair.second)
            assertTrue("$name: $label is ${"%.2f".format(actual)}:1, needs $minimum:1", actual >= minimum)
        }
    }

    private fun assertMode(mode: String, dark: Boolean, black: Boolean) {
        Accent.entries.forEach { accent ->
            val t = tokensFor(dark, black, accent)
            assertAll("$mode $accent", textPairs(t), TEXT_MINIMUM)
            assertAll("$mode $accent", graphicPairs(t), GRAPHIC_MINIMUM)
        }
    }

    @Test
    fun lightMeetsAaForEveryAccent() = assertMode("light", dark = false, black = false)

    @Test
    fun darkMeetsAaForEveryAccent() = assertMode("dark", dark = true, black = false)

    @Test
    fun blackMeetsAaForEveryAccent() = assertMode("black", dark = true, black = true)

    /** Black only darkens the page and its layers; it never changes a light theme. */
    @Test
    fun blackIsIgnoredInLight() = Accent.entries.forEach { accent ->
        assertEquals(tokensFor(false, false, accent), tokensFor(false, true, accent))
    }

    private companion object {
        const val TEXT_MINIMUM = 4.5
        const val GRAPHIC_MINIMUM = 3.0
    }
}
