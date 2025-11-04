package com.sameerasw.doodlist.utils

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import androidx.lifecycle.Lifecycle

object HapticUtil {
    /**
     * Perform a light tick haptic - for subtle interactions like swiping
     */
    fun performLightTick(haptics: HapticFeedback?) {
        try {
            haptics?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {
        }
    }

    /**
     * Perform a medium tick haptic - for toggles and buttons
     */
    fun performClick(haptics: HapticFeedback?) {
        try {
            haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {
        }
    }

    /**
     * Perform a toggle ON haptic - slightly stronger
     */
    fun performToggleOn(haptics: HapticFeedback?) {
        try {
            haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {
        }
    }

    /**
     * Perform a toggle OFF haptic - slightly lighter
     */
    fun performToggleOff(haptics: HapticFeedback?) {
        try {
            haptics?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        } catch (_: Exception) {
        }
    }

    /**
     * Perform a success haptic - 3 quick taps for successful operations
     */
    fun performSuccess(haptics: HapticFeedback?) {
        try {
            CoroutineScope(Dispatchers.Main).launch {
                haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(100)
                haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(50)
                haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Perform a short fade-out haptic (~200ms) used for eraser deletions.
     */
    fun performFadeOut(haptics: HapticFeedback?) {
        try {
            CoroutineScope(Dispatchers.Main).launch {
                // start medium, then two lighter taps to fade out
                haptics?.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(80)
                haptics?.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                delay(120)
                // end
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Perform a variable-strength tick based on [strength] in 0..1.
     * Lower strengths are subtle, higher strengths are firmer.
     */
    fun performVariableTick(haptics: HapticFeedback?, strength: Float) {
        try {
            val s = strength.coerceIn(0f, 1f)
            // Emit 1..3 light ticks with short spacing so perception scales smoothly with strength.
            CoroutineScope(Dispatchers.Main).launch {
                when {
                    s <= 0.33f -> {
                        performLightTick(haptics)
                    }
                    s <= 0.66f -> {
                        performLightTick(haptics)
                        delay(60)
                        performLightTick(haptics)
                    }
                    else -> {
                        performLightTick(haptics)
                        delay(50)
                        performLightTick(haptics)
                        delay(40)
                        performLightTick(haptics)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Start repeating haptic ticks for loading states
     * Returns a Job that can be cancelled to stop the haptics
     *
     * If a lifecycle is provided, haptics will only fire while the lifecycle
     * is at least STARTED (i.e. the app/screen is in foreground). This prevents
     * haptics from triggering while the app is backgrounded.
     */
    fun startLoadingHaptics(haptics: HapticFeedback?, lifecycle: Lifecycle? = null): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                try {
                    val shouldRun = lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true
                    if (shouldRun) {
                        performLightTick(haptics)
                        delay(200) // 5 times per second
                    } else {
                        // Backoff while app is backgrounded; poll until it becomes STARTED again
                        delay(200)
                    }
                } catch (_: Exception) {
                    // swallow any exceptions from lifecycle or haptics and continue
                    delay(200)
                }
            }
        }
    }
}
