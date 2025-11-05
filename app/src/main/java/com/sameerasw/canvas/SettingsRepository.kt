package com.sameerasw.canvas

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple preferences-backed repository to hold settings like Haptics level.
 */
object SettingsRepository {
    private const val PREFS_NAME = "canvas_prefs"
    private const val KEY_HAPTICS_LEVEL = "haptics_level"
    private const val KEY_PIN_TOP_TOOLBAR = "pin_top_toolbar"

    enum class HapticsLevel(val value: Int) {
        OFF(0),
        MIN(1),
        FULL(2);

        companion object {
            fun fromValue(v: Int) = HapticsLevel.entries.firstOrNull { it.value == v } ?: FULL
        }
    }

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getHapticsLevel(): HapticsLevel {
        val p = prefs ?: return HapticsLevel.FULL
        return HapticsLevel.fromValue(p.getInt(KEY_HAPTICS_LEVEL, HapticsLevel.FULL.value))
    }

    fun setHapticsLevel(level: HapticsLevel) {
        prefs?.edit()?.putInt(KEY_HAPTICS_LEVEL, level.value)?.apply()
    }

    // New setting: Pin top toolbar (off by default)
    fun getPinTopToolbar(): Boolean {
        val p = prefs ?: return false
        return p.getBoolean(KEY_PIN_TOP_TOOLBAR, false)
    }

    fun setPinTopToolbar(pin: Boolean) {
        prefs?.edit()?.putBoolean(KEY_PIN_TOP_TOOLBAR, pin)?.apply()
    }
}
