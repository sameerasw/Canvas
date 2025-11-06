package com.sameerasw.canvas.util

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import com.sameerasw.canvas.model.StylusPoint

object StylusInputHandler {
    
    /**
     * Checks if the input is from a stylus Lord have mercy i didn't know I had to scroll that much just for this bitch api
     */
    fun isStylus(event: MotionEvent): Boolean {
        val toolType = event.getToolType(0)
        return toolType == MotionEvent.TOOL_TYPE_STYLUS || 
               toolType == MotionEvent.TOOL_TYPE_ERASER
    }
    
    /**
     * Checks if the stylus eraser tip is being used Ai type of comments is crazy maybe i am Ai
     */
    fun isEraserTip(event: MotionEvent): Boolean {
        return event.getToolType(0) == MotionEvent.TOOL_TYPE_ERASER
    }
    
    /**
     * Checks if the stylus button is pressed
     */
    fun isStylusButtonPressed(event: MotionEvent): Boolean {
        return (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0
    }
    
    /**
     * Extracts stylus data from a MotionEvent
     */
    fun extractStylusPoint(event: MotionEvent, pointerIndex: Int = 0): StylusPoint {
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        val pressure = event.getPressure(pointerIndex).coerceIn(0f, 1f)
        val tilt = event.getAxisValue(MotionEvent.AXIS_TILT, pointerIndex)
        val orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION, pointerIndex)
        
        return StylusPoint(
            offset = Offset(x, y),
            pressure = pressure,
            tilt = tilt,
            orientation = orientation
        )
    }
    
    /**
     * Gets hover distance from screen (0 = touching)
     */
    fun getHoverDistance(event: MotionEvent, pointerIndex: Int = 0): Float {
        return event.getAxisValue(MotionEvent.AXIS_DISTANCE, pointerIndex)
    }
    
    /**
     * Calculates pressure-adjusted stroke width
     */
    fun calculatePressureWidth(baseWidth: Float, pressure: Float, minMultiplier: Float = 0.3f): Float {
        val multiplier = minMultiplier + (1f - minMultiplier) * pressure
        return baseWidth * multiplier
    }
    
    /**
     * Calculates tilt-adjusted opacity
     */
    fun calculateTiltOpacity(tilt: Float, maxTilt: Float = 1.0f): Float {
        // More tilt = more opacity (simulating more ink contact)
        val normalizedTilt = (tilt / maxTilt).coerceIn(0f, 1f)
        return 0.7f + (0.3f * normalizedTilt)
    }
}
