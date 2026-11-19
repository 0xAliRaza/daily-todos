package com.aliraza.dailytodos.core.focus

import com.aliraza.dailytodos.core.model.TimerSignal

/**
 * How a round's end feels, as Android waveform timings: off, on, off, on… in milliseconds.
 *
 * With sound on, each sound gets one short buzz ([withSound]). Vibrate-only has to stand in for the
 * sound, so it is long and strong but still not an alarm: [FocusRounds.pulses] full buzzes, a pause,
 * and the same again ([alone]): about 8 s at a focus end and 6 s at a break end.
 */
object EndBuzz {
    const val WITH_SOUND_MS = 400L
    const val BUZZ_MS = 900L
    const val GAP_MS = 450L
    const val GROUP_PAUSE_MS = 1_200L
    private const val GROUPS = 2

    /** Full strength, where the vibrator can vary it. */
    const val AMPLITUDE = 255

    fun withSound(): LongArray = longArrayOf(0, WITH_SOUND_MS)

    fun alone(ended: FocusPhase): LongArray {
        val pulses = FocusRounds.pulses(ended)
        val timings = mutableListOf<Long>()
        repeat(GROUPS) { group ->
            repeat(pulses) { pulse ->
                timings += when {
                    pulse > 0 -> GAP_MS
                    group > 0 -> GROUP_PAUSE_MS
                    else -> 0L
                }
                timings += BUZZ_MS
            }
        }
        return timings.toLongArray()
    }

    /** Amplitudes matching [timings]: silent while off, [AMPLITUDE] while on. */
    fun amplitudes(timings: LongArray): IntArray = IntArray(timings.size) { if (it % 2 == 0) 0 else AMPLITUDE }
}

/** The phone's sound mode, as the end signal sees it. */
enum class Ringer { NORMAL, VIBRATE, SILENT }

/** What a round's end plays: the sound or not, and which buzz, if any. */
data class EndPlan(val sound: Boolean, val buzz: Buzz?) {
    enum class Buzz { WITH_SOUND, ALONE }

    companion object {
        private val Quiet = EndPlan(sound = false, buzz = null)
        private val BuzzOnly = EndPlan(sound = false, buzz = Buzz.ALONE)

        /**
         * The picked [signal] as the phone's [ringer] allows it: vibrate mode turns any signal into the long
         * buzz, silent mode keeps it quiet. With [asAlarm] (the Do Not Disturb override) the sound mode is
         * ignored and the signal plays as picked.
         */
        fun of(signal: TimerSignal, ringer: Ringer, asAlarm: Boolean): EndPlan = when {
            asAlarm || ringer == Ringer.NORMAL -> when (signal) {
                TimerSignal.SOUND -> EndPlan(sound = true, buzz = null)
                TimerSignal.VIBRATE -> BuzzOnly
                TimerSignal.BOTH -> EndPlan(sound = true, buzz = Buzz.WITH_SOUND)
            }

            ringer == Ringer.VIBRATE -> BuzzOnly

            else -> Quiet
        }
    }
}
