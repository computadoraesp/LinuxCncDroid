package com.example.service

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class CncFeedbackManager(context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val attributionContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            context.createAttributionContext("vibration")
        } catch (_: Exception) {
            context
        }
    } else {
        context
    }

    private val vibrator: Vibrator? by lazy {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = attributionContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                attributionContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (_: Exception) {
            // Audio service may be limited in some sandbox setups
        }
    }

    // --- HAPTIC FEEDBACK PATTERNS ---

    fun triggerJogTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(15)
            }
        } catch (_: Exception) {}
    }

    fun triggerActionClick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    fun triggerEstopHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 150, 80, 150, 80, 250)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 150, 80, 150, 80, 250), -1)
            }
        } catch (_: Exception) {}
    }

    fun triggerWarningHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 80, 50, 80)
                val amplitudes = intArrayOf(0, 200, 0, 200)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 80, 50, 80), -1)
            }
        } catch (_: Exception) {}
    }

    fun triggerSuccessHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 30, 40, 30), -1)
            }
        } catch (_: Exception) {}
    }

    // --- ACOUSTIC AUDIO SIGNALS ---

    fun playErrorAlarm() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 350)
                delay(400.milliseconds)
                toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 350)
            } catch (_: Exception) {}
        }
    }

    fun playEstopSound() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 500)
            } catch (_: Exception) {}
        }
    }

    fun playWarningBeep() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
            } catch (_: Exception) {}
        }
    }

    fun playProbeTripSound() {
        scope.launch {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 90)
            } catch (_: Exception) {}
        }
    }

    fun playCycleCompleteSound() {
        scope.launch {
            try {
                // Three ascending beeps — unmistakably "done"
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                delay(220.milliseconds)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                delay(220.milliseconds)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 350)
            } catch (_: Exception) {}
        }
    }

    /**
     * Distinctive low-battery alert: two firm pulses separated by a short gap.
     * Deliberately different from the ESTOP waveform so the operator can identify it
     * without looking at the screen.
     */
    fun playLowBatteryAlert() {
        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings    = longArrayOf(0, 120, 100, 120)
                    val amplitudes = intArrayOf(0, 180, 0, 180)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 120, 100, 120), -1)
                }
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            } catch (_: Exception) {}
        }
    }

    /**
     * Stronger success pattern for cycle completion haptic —
     * three pulses so it is felt in a pocket.
     */
    fun playCycleCompleteHaptic() {
        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings    = longArrayOf(0, 80, 60, 80, 60, 200)
                    val amplitudes = intArrayOf(0, 160, 0, 160, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 80, 60, 80, 60, 200), -1)
                }
            } catch (_: Exception) {}
        }
    }
}
