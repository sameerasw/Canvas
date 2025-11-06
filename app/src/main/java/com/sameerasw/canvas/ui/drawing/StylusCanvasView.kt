package com.sameerasw.canvas.ui.drawing

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.sameerasw.canvas.model.StylusPoint
import com.sameerasw.canvas.util.StylusInputHandler

class StylusCanvasView(context: Context) : View(context) {
    
    private val currentStylusPoints = mutableListOf<StylusPoint>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    
    var onStylusStrokeComplete: ((List<StylusPoint>) -> Unit)? = null
    var onStylusButtonPressed: (() -> Unit)? = null
    var onHoverMove: ((Offset) -> Unit)? = null
    var currentColor: Color = Color.Black
    var currentWidth: Float = 5f
    var isStylusEnabled: Boolean = true
    
    private var isStylusButtonActive = false
    private var hoverPosition: Offset? = null
    private val hoverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    

    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isStylusEnabled) return false
        
        val isStylus = StylusInputHandler.isStylus(event)
        val isEraserTip = StylusInputHandler.isEraserTip(event)
        val isStylusButton = StylusInputHandler.isStylusButtonPressed(event)
        
        // Handle stylus button press (toggle eraser mode)
        if (isStylus && isStylusButton && !isStylusButtonActive) {
            isStylusButtonActive = true
            onStylusButtonPressed?.invoke()
        } else if (!isStylusButton) {
            isStylusButtonActive = false
        }
        
        // If eraser tip is detected, trigger eraser mode
        if (isEraserTip && !isStylusButtonActive) {
            onStylusButtonPressed?.invoke()
            isStylusButtonActive = true
        }
        
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (isStylus) {
                    currentStylusPoints.clear()
                    val point = StylusInputHandler.extractStylusPoint(event)
                    currentStylusPoints.add(point)
                    path.reset()
                    path.moveTo(point.offset.x, point.offset.y)
                    return true
                }
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isStylus && currentStylusPoints.isNotEmpty()) {
                    val point = StylusInputHandler.extractStylusPoint(event)
                    currentStylusPoints.add(point)
                    path.lineTo(point.offset.x, point.offset.y)
                    invalidate()
                    return true
                }
            }
            
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isStylus && currentStylusPoints.isNotEmpty()) {
                    onStylusStrokeComplete?.invoke(currentStylusPoints.toList())
                    currentStylusPoints.clear()
                    path.reset()
                    invalidate()
                    return true
                }
            }
        }
        
        return false
    }
    
    override fun onHoverEvent(event: MotionEvent): Boolean {
        if (!isStylusEnabled) return false
        
        val isStylus = StylusInputHandler.isStylus(event)
        
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                if (isStylus) {
                    hoverPosition = Offset(event.x, event.y)
                    onHoverMove?.invoke(hoverPosition!!)
                    invalidate()
                    return true
                }
            }
            
            MotionEvent.ACTION_HOVER_EXIT -> {
                hoverPosition = null
                invalidate()
                return true
            }
        }
        
        return super.onHoverEvent(event)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Draw current stroke with pressure sensitivity
        if (currentStylusPoints.size >= 2) {
            paint.color = currentColor.toArgb()
            
            for (i in 0 until currentStylusPoints.size - 1) {
                val start = currentStylusPoints[i]
                val end = currentStylusPoints[i + 1]
                
                val avgPressure = (start.pressure + end.pressure) / 2f
                paint.strokeWidth = currentWidth * (0.3f + 0.7f * avgPressure)
                
                canvas.drawLine(
                    start.offset.x, start.offset.y,
                    end.offset.x, end.offset.y,
                    paint
                )
            }
        }
        
        // Draw hover cursor
        hoverPosition?.let { pos ->
            hoverPaint.color = currentColor.copy(alpha = 0.4f).toArgb()
            canvas.drawCircle(pos.x, pos.y, currentWidth / 2f, hoverPaint)
        }
    }
}
