package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.model.TimerSignal
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EndBuzzTest {
    private fun LongArray.buzzes() = filterIndexed { i, _ -> i % 2 == 1 }

    @Test
    fun vibrateOnlyIsTwoGroupsOfLongBuzzes() {
        val focus = EndBuzz.alone(FocusPhase.FOCUS)
        assertArrayEquals(longArrayOf(0, 900, 450, 900, 450, 900, 1_200, 900, 450, 900, 450, 900), focus)
        assertEquals(6, focus.buzzes().size)
        assertEquals(4, EndBuzz.alone(FocusPhase.BREAK).buzzes().size)
    }

    @Test
    fun itLastsLongerThanANotificationButIsNoAlarm() {
        assertEquals(8_400L, EndBuzz.alone(FocusPhase.FOCUS).sum())
        assertEquals(5_700L, EndBuzz.alone(FocusPhase.BREAK).sum())
    }

    @Test
    fun withSoundEachSoundGetsOneShortBuzz() {
        assertArrayEquals(longArrayOf(0, 400), EndBuzz.withSound())
    }

    @Test
    fun buzzesAreFullStrengthAndPausesSilent() {
        val timings = EndBuzz.alone(FocusPhase.BREAK)
        val amplitudes = EndBuzz.amplitudes(timings)
        assertEquals(timings.size, amplitudes.size)
        amplitudes.forEachIndexed { i, a -> assertEquals(if (i % 2 == 0) 0 else 255, a) }
    }
}

class EndPlanTest {
    @Test
    fun aNormalPhonePlaysTheSignalAsPicked() {
        assertEquals(EndPlan(true, null), EndPlan.of(TimerSignal.SOUND, Ringer.NORMAL, asAlarm = false))
        assertEquals(
            EndPlan(false, EndPlan.Buzz.ALONE),
            EndPlan.of(TimerSignal.VIBRATE, Ringer.NORMAL, asAlarm = false),
        )
        assertEquals(
            EndPlan(true, EndPlan.Buzz.WITH_SOUND),
            EndPlan.of(TimerSignal.BOTH, Ringer.NORMAL, asAlarm = false),
        )
    }

    @Test
    fun vibrateModeBuzzesAndSilentModeStaysQuiet() {
        TimerSignal.entries.forEach { signal ->
            assertEquals(EndPlan(false, EndPlan.Buzz.ALONE), EndPlan.of(signal, Ringer.VIBRATE, asAlarm = false))
            assertEquals(EndPlan(false, null), EndPlan.of(signal, Ringer.SILENT, asAlarm = false))
        }
    }

    @Test
    fun theDoNotDisturbOverridePlaysAsPickedEvenWhenSilent() {
        assertEquals(
            EndPlan(true, EndPlan.Buzz.WITH_SOUND),
            EndPlan.of(TimerSignal.BOTH, Ringer.SILENT, asAlarm = true),
        )
    }
}
