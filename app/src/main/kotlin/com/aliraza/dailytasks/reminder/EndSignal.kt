package com.aliraza.dailytasks.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.aliraza.dailytasks.core.focus.EndBuzz
import com.aliraza.dailytasks.core.focus.EndPlan
import com.aliraza.dailytasks.core.focus.FocusPhase
import com.aliraza.dailytasks.core.focus.FocusRounds
import com.aliraza.dailytasks.core.focus.Ringer
import com.aliraza.dailytasks.core.model.TimerSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A round's end, heard and felt (see [EndPlan] and [EndBuzz]): the phone's own notification sound a few
 * times with a short buzz each, or a long strong buzz alone, then quiet. The phone's sound mode is
 * respected unless [play] is asked to play as an alarm, the Do Not Disturb override.
 */
class EndSignal(private val context: Context, private val scope: CoroutineScope) {
    private var playing: Job? = null

    fun play(ended: FocusPhase, signal: TimerSignal, asAlarm: Boolean) {
        playing?.cancel()
        val plan = EndPlan.of(signal, ringer(), asAlarm)
        when {
            plan.sound -> playing = scope.launch { sound(FocusRounds.pulses(ended), plan.buzz != null, asAlarm) }
            plan.buzz != null -> vibrate(EndBuzz.alone(ended), asAlarm)
        }
    }

    private suspend fun sound(pulses: Int, buzz: Boolean, asAlarm: Boolean) {
        val tone = tone(asAlarm) ?: return
        try {
            repeat(pulses) {
                tone.play()
                if (buzz) vibrate(EndBuzz.withSound(), asAlarm)
                // Wait for the sound to finish, but never longer than a long notification sound takes.
                withTimeoutOrNull(MAX_SOUND_MS) { while (tone.isPlaying) delay(POLL_MS) }
                delay(GAP_MS)
            }
        } finally {
            tone.stop()
        }
    }

    private fun tone(asAlarm: Boolean): Ringtone? {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: return null
        return RingtoneManager.getRingtone(context, uri)?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(if (asAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }
    }

    private fun vibrate(timings: LongArray, asAlarm: Boolean) {
        val vibrator = vibrator()?.takeIf { it.hasVibrator() } ?: return
        val effect = if (vibrator.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(timings, EndBuzz.amplitudes(timings), NO_REPEAT)
        } else {
            VibrationEffect.createWaveform(timings, NO_REPEAT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val usage = if (asAlarm) VibrationAttributes.USAGE_ALARM else VibrationAttributes.USAGE_NOTIFICATION
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(usage))
        } else {
            vibrator.vibrate(effect)
        }
    }

    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }

    private fun ringer(): Ringer = when (context.getSystemService(AudioManager::class.java)?.ringerMode) {
        AudioManager.RINGER_MODE_VIBRATE -> Ringer.VIBRATE
        AudioManager.RINGER_MODE_SILENT -> Ringer.SILENT
        else -> Ringer.NORMAL
    }

    private companion object {
        const val MAX_SOUND_MS = 4_000L
        const val POLL_MS = 100L
        const val GAP_MS = 250L
        const val NO_REPEAT = -1
    }
}
