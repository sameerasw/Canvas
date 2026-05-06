package com.sameerasw.canvas.util

import android.content.Context
import android.os.Build
import android.os.PowerManager

object DeviceUtils {
    /**
     * Samsung devices on One UI 7 (Android 15) or below have a broken blur implementation
     * that causes a gray screen overlay. Disable it for them. (╯°□°）╯︵ ┻━┻
     */
    fun isBlurProblematicDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true) && 
                Build.VERSION.SDK_INT <= 35 // Android 15
    }

    fun isPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode == true
    }
}
