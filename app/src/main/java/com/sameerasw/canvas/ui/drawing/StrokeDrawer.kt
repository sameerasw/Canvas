package com.sameerasw.canvas.ui.drawing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.sameerasw.canvas.model.PenStyle
import com.sameerasw.canvas.model.ShapeType
import com.sameerasw.canvas.model.StylusPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object StrokeDrawer {
    fun DrawScope.drawScribbleStroke(stroke: List<Offset>, color: Color, width: Float, style: PenStyle = PenStyle.NORMAL) {
        if (stroke.size < 2) return

        when (style) {
            PenStyle.NORMAL -> drawNormalStroke(stroke, color, width)
            PenStyle.SMOOTH -> drawSmoothStroke(stroke, color, width)
            PenStyle.ROUGH -> drawRoughStroke(stroke, color, width)
        }
    }
    
    fun DrawScope.drawPressureSensitiveStroke(
        stylusPoints: List<StylusPoint>,
        color: Color,
        baseWidth: Float,
        style: PenStyle = PenStyle.NORMAL
    ) {
        if (stylusPoints.size < 2) return
        
        when (style) {
            PenStyle.NORMAL -> drawPressureNormalStroke(stylusPoints, color, baseWidth)
            PenStyle.SMOOTH -> drawPressureSmoothStroke(stylusPoints, color, baseWidth)
            PenStyle.ROUGH -> drawPressureRoughStroke(stylusPoints, color, baseWidth)
        }
    }
    
    private fun DrawScope.drawPressureNormalStroke(
        stylusPoints: List<StylusPoint>,
        color: Color,
        baseWidth: Float
    ) {
        // Draw segments with varying width based on pressure
        for (i in 0 until stylusPoints.size - 1) {
            val start = stylusPoints[i]
            val end = stylusPoints[i + 1]
            
            val avgPressure = (start.pressure + end.pressure) / 2f
            val segmentWidth = baseWidth * (0.3f + 0.7f * avgPressure)
            
            // Apply tilt for opacity variation
            val avgTilt = (start.tilt + end.tilt) / 2f
            val tiltOpacity = 0.85f + (0.15f * (avgTilt / 1.0f).coerceIn(0f, 1f))
            
            drawLine(
                color = color.copy(alpha = color.alpha * tiltOpacity),
                start = start.offset,
                end = end.offset,
                strokeWidth = segmentWidth
            )
        }
    }
    
    private fun DrawScope.drawPressureSmoothStroke(
        stylusPoints: List<StylusPoint>,
        color: Color,
        baseWidth: Float
    ) {
        // Create smooth path with pressure-varying width
        val path = Path()
        path.moveTo(stylusPoints.first().offset.x, stylusPoints.first().offset.y)
        
        for (i in 1 until stylusPoints.size) {
            val prev = stylusPoints[i - 1]
            val curr = stylusPoints[i]
            val midX = (prev.offset.x + curr.offset.x) / 2f
            val midY = (prev.offset.y + curr.offset.y) / 2f
            path.quadraticTo(prev.offset.x, prev.offset.y, midX, midY)
        }
        path.lineTo(stylusPoints.last().offset.x, stylusPoints.last().offset.y)
        
        // Use average pressure for the whole stroke
        val avgPressure = stylusPoints.map { it.pressure }.average().toFloat()
        val strokeWidth = baseWidth * (0.3f + 0.7f * avgPressure)
        
        drawPath(path, color, style = Stroke(width = strokeWidth))
    }
    
    private fun DrawScope.drawPressureRoughStroke(
        stylusPoints: List<StylusPoint>,
        color: Color,
        baseWidth: Float
    ) {
        for (i in 0 until stylusPoints.size - 1) {
            val start = stylusPoints[i]
            val end = stylusPoints[i + 1]
            
            val avgPressure = (start.pressure + end.pressure) / 2f
            val segmentWidth = baseWidth * (0.3f + 0.7f * avgPressure)
            val jitter = segmentWidth * 0.3f
            
            val offsetStart = Offset(
                start.offset.x + Random.nextFloat() * jitter - jitter / 2,
                start.offset.y + Random.nextFloat() * jitter - jitter / 2
            )
            val offsetEnd = Offset(
                end.offset.x + Random.nextFloat() * jitter - jitter / 2,
                end.offset.y + Random.nextFloat() * jitter - jitter / 2
            )
            
            drawLine(color, offsetStart, offsetEnd, strokeWidth = segmentWidth * 0.8f)
        }
    }

    private fun DrawScope.drawNormalStroke(stroke: List<Offset>, color: Color, width: Float) {
        val baseWidth = width.coerceAtLeast(1.0f)
        val layerOffsets = listOf(-0.4f, 0f, 0.4f)

        layerOffsets.forEachIndexed { offsetIndex, layerFactor ->
            val layerOffset = layerFactor * baseWidth
            val path = Path()
            path.moveTo(stroke.first().x + layerOffset, stroke.first().y + layerOffset)

            for (i in 1 until stroke.size) {
                val prev = stroke[i - 1]
                val curr = stroke[i]
                val midX = (prev.x + curr.x) / 2f + layerOffset
                val midY = (prev.y + curr.y) / 2f + layerOffset
                path.quadraticTo(prev.x + layerOffset, prev.y + layerOffset, midX, midY)
            }
            path.lineTo(stroke.last().x + layerOffset, stroke.last().y + layerOffset)

            val widthFactor = 1f - (abs(offsetIndex - 1) * 0.25f)
            val strokeWidth = baseWidth * (0.6f + 0.4f * widthFactor)
            drawPath(path, color.copy(alpha = 0.55f + 0.25f * widthFactor), style = Stroke(width = strokeWidth))
        }

        val mainPath = Path()
        mainPath.moveTo(stroke.first().x, stroke.first().y)
        for (i in 1 until stroke.size) {
            val prev = stroke[i - 1]
            val curr = stroke[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            mainPath.quadraticTo(prev.x, prev.y, midX, midY)
        }
        mainPath.lineTo(stroke.last().x, stroke.last().y)

        drawPath(mainPath, color, style = Stroke(width = baseWidth))
    }

    private fun DrawScope.drawSmoothStroke(stroke: List<Offset>, color: Color, width: Float) {
        val baseWidth = width.coerceAtLeast(1.0f)
        val path = Path()
        path.moveTo(stroke.first().x, stroke.first().y)

        for (i in 1 until stroke.size) {
            val prev = stroke[i - 1]
            val curr = stroke[i]
            val midX = (prev.x + curr.x) / 2f
            val midY = (prev.y + curr.y) / 2f
            path.quadraticTo(prev.x, prev.y, midX, midY)
        }
        path.lineTo(stroke.last().x, stroke.last().y)

        drawPath(path, color, style = Stroke(width = baseWidth))
    }

    private fun DrawScope.drawRoughStroke(stroke: List<Offset>, color: Color, width: Float) {
        val baseWidth = width.coerceAtLeast(1.0f)
        
        for (i in 0 until stroke.size - 1) {
            val start = stroke[i]
            val end = stroke[i + 1]
            val jitter = baseWidth * 0.3f
            
            val offsetStart = Offset(
                start.x + Random.nextFloat() * jitter - jitter / 2,
                start.y + Random.nextFloat() * jitter - jitter / 2
            )
            val offsetEnd = Offset(
                end.x + Random.nextFloat() * jitter - jitter / 2,
                end.y + Random.nextFloat() * jitter - jitter / 2
            )
            
            drawLine(color, offsetStart, offsetEnd, strokeWidth = baseWidth * 0.8f)
        }
    }

    fun DrawScope.drawArrow(start: Offset, end: Offset, color: Color, width: Float) {
        // Calculate arrow head
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val arrowLength = (width * 3.5f).coerceAtLeast(12f)
        val arrowAngle = Math.PI / 7
        
        val arrow1 = Offset(
            (end.x - arrowLength * cos(angle - arrowAngle)).toFloat(),
            (end.y - arrowLength * sin(angle - arrowAngle)).toFloat()
        )
        val arrow2 = Offset(
            (end.x - arrowLength * cos(angle + arrowAngle)).toFloat(),
            (end.y - arrowLength * sin(angle + arrowAngle)).toFloat()
        )
        
        // Calculate where the line should stop (at the base of the arrow)
        val arrowBase = Offset(
            (end.x - arrowLength * 0.7f * cos(angle)).toFloat(),
            (end.y - arrowLength * 0.7f * sin(angle)).toFloat()
        )
        
        // Draw the main line (stopping before the arrow head)
        drawLine(color, start, arrowBase, strokeWidth = width)
        
        // Draw filled arrow head as a triangle
        val path = Path().apply {
            moveTo(end.x, end.y)
            lineTo(arrow1.x, arrow1.y)
            lineTo(arrow2.x, arrow2.y)
            close()
        }
        drawPath(path, color)
    }

    fun DrawScope.drawShape(start: Offset, end: Offset, shapeType: ShapeType, color: Color, width: Float, isFilled: Boolean = false) {
        when (shapeType) {
            ShapeType.RECTANGLE -> {
                val rect = Rect(start, end)
                if (isFilled) {
                    drawRect(color, topLeft = rect.topLeft, size = rect.size)
                } else {
                    drawRect(color, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = width))
                }
            }
            ShapeType.CIRCLE -> {
                val dx = end.x - start.x
                val dy = end.y - start.y
                val radius = sqrt(dx * dx + dy * dy)
                if (isFilled) {
                    drawCircle(color, radius = radius, center = start)
                } else {
                    drawCircle(color, radius = radius, center = start, style = Stroke(width = width))
                }
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(start.x, end.y)
                    lineTo((start.x + end.x) / 2f, start.y)
                    lineTo(end.x, end.y)
                    close()
                }
                if (isFilled) {
                    drawPath(path, color)
                } else {
                    drawPath(path, color, style = Stroke(width = width))
                }
            }
            ShapeType.LINE -> {
                drawLine(color, start, end, strokeWidth = width)
            }
        }
    }
}

