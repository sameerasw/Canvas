package com.sameerasw.canvas.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.canvas.R
import com.sameerasw.canvas.model.DrawStroke
import com.sameerasw.canvas.data.TextItem
import com.sameerasw.canvas.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale

object BitmapExportHelper {
    suspend fun createBitmapFromData(
        context: Context,
        strokes: List<DrawStroke>,
        texts: List<TextItem>,
        outputWidth: Int = 2048,
        outputHeight: Int = 2048
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (strokes.isEmpty() && texts.isEmpty()) return@withContext null

        val bmp = createBitmap(outputWidth, outputHeight)
        val canvas = AndroidCanvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)


        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { s ->
            if (s.points.size < 2) return@forEach
            paint.color = s.color.toArgb()
            paint.strokeWidth = s.width

            when {
                s.isArrow -> {
                    android.util.Log.d("BitmapExport", "Drawing arrow: points=${s.points.size}, color=${s.color}, width=${s.width}")
                    val start = s.points.first()
                    val end = s.points.last()
                    
                    // Calculate arrow head parameters
                    val angle = kotlin.math.atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                    val arrowLength = (s.width * 5f).coerceAtLeast(20f)  // Made larger for testing
                    val arrowAngle = Math.PI / 6  // Wider angle
                    
                    // Calculate arrow base (where line should stop)
                    val arrowBaseX = (end.x - arrowLength * 0.7f * kotlin.math.cos(angle)).toFloat()
                    val arrowBaseY = (end.y - arrowLength * 0.7f * kotlin.math.sin(angle)).toFloat()
                    
                    // Draw the main line (stopping at arrow base)
                    canvas.drawLine(start.x, start.y, arrowBaseX, arrowBaseY, paint)
                    
                    // Calculate arrow head points
                    val arrow1X = (end.x - arrowLength * kotlin.math.cos(angle - arrowAngle)).toFloat()
                    val arrow1Y = (end.y - arrowLength * kotlin.math.sin(angle - arrowAngle)).toFloat()
                    val arrow2X = (end.x - arrowLength * kotlin.math.cos(angle + arrowAngle)).toFloat()
                    val arrow2Y = (end.y - arrowLength * kotlin.math.sin(angle + arrowAngle)).toFloat()
                    
                    android.util.Log.d("BitmapExport", "Arrow head: tip=($end.x,$end.y), p1=($arrow1X,$arrow1Y), p2=($arrow2X,$arrow2Y)")
                    
                    // Draw filled arrow head
                    val arrowPaint = Paint().apply {
                        color = s.color.toArgb()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val arrowPath = AndroidPath()
                    arrowPath.moveTo(end.x, end.y)
                    arrowPath.lineTo(arrow1X, arrow1Y)
                    arrowPath.lineTo(arrow2X, arrow2Y)
                    arrowPath.close()
                    canvas.drawPath(arrowPath, arrowPaint)
                }
                s.shapeType != null -> {
                    val start = s.points.first()
                    val end = s.points.last()
                    val path = AndroidPath()
                    
                    paint.style = if (s.isFilled) Paint.Style.FILL else Paint.Style.STROKE
                    
                    when (s.shapeType) {
                        com.sameerasw.canvas.model.ShapeType.RECTANGLE -> {
                            path.addRect(start.x, start.y, end.x, end.y, AndroidPath.Direction.CW)
                        }
                        com.sameerasw.canvas.model.ShapeType.CIRCLE -> {
                            val dx = end.x - start.x
                            val dy = end.y - start.y
                            val radius = kotlin.math.sqrt(dx * dx + dy * dy)
                            path.addCircle(start.x, start.y, radius, AndroidPath.Direction.CW)
                        }
                        com.sameerasw.canvas.model.ShapeType.TRIANGLE -> {
                            path.moveTo(start.x, end.y)
                            path.lineTo((start.x + end.x) / 2f, start.y)
                            path.lineTo(end.x, end.y)
                            path.close()
                        }
                        com.sameerasw.canvas.model.ShapeType.LINE -> {
                            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
                        }
                    }
                    if (s.shapeType != com.sameerasw.canvas.model.ShapeType.LINE) {
                        canvas.drawPath(path, paint)
                    }
                    paint.style = Paint.Style.STROKE
                }
                else -> {
                    val path = AndroidPath()
                    val pts = s.points
                    path.moveTo(pts.first().x, pts.first().y)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val curr = pts[i]
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        path.quadTo(prev.x, prev.y, midX, midY)
                    }
                    path.lineTo(pts.last().x, pts.last().y)
                    canvas.drawPath(path, paint)
                }
            }
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        texts.forEach { t ->
            textPaint.color = t.color.toArgb()
            textPaint.textSize = t.size
            try {
                val tf: Typeface? = ResourcesCompat.getFont(context, R.font.font)
                if (tf != null) textPaint.typeface = tf
            } catch (_: Exception) { }

            val fm = textPaint.fontMetrics
            val baseline = t.y - fm.ascent
            canvas.drawText(t.text, t.x, baseline, textPaint)
        }

        return@withContext bmp
    }

    // Renders strokes/texts onto a bitmap sized to viewWidth x viewHeight using the provided
    // transform (scale + offsets), then crops the given rectangle (in view pixel coords) and
    // scales the cropped region to outputWidth/outputHeight.
    suspend fun createBitmapFromDataWithViewport(
        context: Context,
        strokes: List<DrawStroke>,
        texts: List<TextItem>,
        viewWidth: Int,
        viewHeight: Int,
        transformScale: Float,
        transformOffsetX: Float,
        transformOffsetY: Float,
        cropLeft: Int,
        cropTop: Int,
        cropWidth: Int,
        cropHeight: Int,
        outputWidth: Int,
        outputHeight: Int
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (strokes.isEmpty() && texts.isEmpty()) return@withContext null

        // Render full view-sized bitmap that matches the on-screen canvas rendering
        val full = createBitmap(viewWidth.coerceAtLeast(1), viewHeight.coerceAtLeast(1))
        val canvas = AndroidCanvas(full)
        canvas.drawColor(android.graphics.Color.WHITE)


        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { s ->
            if (s.points.size < 2) return@forEach
            paint.color = s.color.toArgb()
            paint.strokeWidth = s.width * transformScale

            when {
                s.isArrow -> {
                    android.util.Log.d("BitmapExport", "Drawing arrow (viewport): points=${s.points.size}, color=${s.color}, width=${s.width}")
                    val start = s.points.first()
                    val end = s.points.last()
                    val startX = start.x * transformScale + transformOffsetX
                    val startY = start.y * transformScale + transformOffsetY
                    val endX = end.x * transformScale + transformOffsetX
                    val endY = end.y * transformScale + transformOffsetY
                    
                    // Calculate arrow head parameters
                    val angle = kotlin.math.atan2((endY - startY).toDouble(), (endX - startX).toDouble())
                    val arrowLength = (s.width * transformScale * 5f).coerceAtLeast(20f)  // Made larger for testing
                    val arrowAngle = Math.PI / 6  // Wider angle
                    
                    // Calculate arrow base (where line should stop)
                    val arrowBaseX = (endX - arrowLength * 0.7f * kotlin.math.cos(angle)).toFloat()
                    val arrowBaseY = (endY - arrowLength * 0.7f * kotlin.math.sin(angle)).toFloat()
                    
                    // Draw the main line (stopping at arrow base)
                    canvas.drawLine(startX, startY, arrowBaseX, arrowBaseY, paint)
                    
                    // Calculate arrow head points
                    val arrow1X = (endX - arrowLength * kotlin.math.cos(angle - arrowAngle)).toFloat()
                    val arrow1Y = (endY - arrowLength * kotlin.math.sin(angle - arrowAngle)).toFloat()
                    val arrow2X = (endX - arrowLength * kotlin.math.cos(angle + arrowAngle)).toFloat()
                    val arrow2Y = (endY - arrowLength * kotlin.math.sin(angle + arrowAngle)).toFloat()
                    
                    // Draw filled arrow head
                    val arrowPaint = Paint().apply {
                        color = s.color.toArgb()
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val arrowPath = AndroidPath()
                    arrowPath.moveTo(endX, endY)
                    arrowPath.lineTo(arrow1X, arrow1Y)
                    arrowPath.lineTo(arrow2X, arrow2Y)
                    arrowPath.close()
                    canvas.drawPath(arrowPath, arrowPaint)
                }
                s.shapeType != null -> {
                    val start = s.points.first()
                    val end = s.points.last()
                    val startX = start.x * transformScale + transformOffsetX
                    val startY = start.y * transformScale + transformOffsetY
                    val endX = end.x * transformScale + transformOffsetX
                    val endY = end.y * transformScale + transformOffsetY
                    val path = AndroidPath()
                    
                    paint.style = if (s.isFilled) Paint.Style.FILL else Paint.Style.STROKE
                    
                    when (s.shapeType) {
                        com.sameerasw.canvas.model.ShapeType.RECTANGLE -> {
                            path.addRect(startX, startY, endX, endY, AndroidPath.Direction.CW)
                        }
                        com.sameerasw.canvas.model.ShapeType.CIRCLE -> {
                            val dx = endX - startX
                            val dy = endY - startY
                            val radius = kotlin.math.sqrt(dx * dx + dy * dy)
                            path.addCircle(startX, startY, radius, AndroidPath.Direction.CW)
                        }
                        com.sameerasw.canvas.model.ShapeType.TRIANGLE -> {
                            path.moveTo(startX, endY)
                            path.lineTo((startX + endX) / 2f, startY)
                            path.lineTo(endX, endY)
                            path.close()
                        }
                        com.sameerasw.canvas.model.ShapeType.LINE -> {
                            canvas.drawLine(startX, startY, endX, endY, paint)
                        }
                    }
                    if (s.shapeType != com.sameerasw.canvas.model.ShapeType.LINE) {
                        canvas.drawPath(path, paint)
                    }
                    paint.style = Paint.Style.STROKE
                }
                else -> {
                    val path = AndroidPath()
                    val pts = s.points
                    path.moveTo(pts.first().x * transformScale + transformOffsetX, pts.first().y * transformScale + transformOffsetY)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val curr = pts[i]
                        val prevX = prev.x * transformScale + transformOffsetX
                        val prevY = prev.y * transformScale + transformOffsetY
                        val currX = curr.x * transformScale + transformOffsetX
                        val currY = curr.y * transformScale + transformOffsetY
                        val midX = (prevX + currX) / 2f
                        val midY = (prevY + currY) / 2f
                        path.quadTo(prevX, prevY, midX, midY)
                    }
                    val last = pts.last()
                    path.lineTo(last.x * transformScale + transformOffsetX, last.y * transformScale + transformOffsetY)
                    canvas.drawPath(path, paint)
                }
            }
        }

        val textPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        texts.forEach { t ->
            textPaint.color = t.color.toArgb()
            textPaint.textSize = t.size * transformScale
            try {
                val tf: Typeface? = ResourcesCompat.getFont(context, R.font.font)
                if (tf != null) textPaint.typeface = tf
            } catch (_: Exception) { }

            val sx = t.x * transformScale + transformOffsetX
            val sy = t.y * transformScale + transformOffsetY
            val fm = textPaint.fontMetrics
            val baseline = sy - fm.ascent
            canvas.drawText(t.text, sx, baseline, textPaint)
        }

        // Clip crop rect to bitmap bounds
        val left = cropLeft.coerceIn(0, full.width - 1)
        val top = cropTop.coerceIn(0, full.height - 1)
        val w = cropWidth.coerceIn(1, full.width - left)
        val h = cropHeight.coerceIn(1, full.height - top)

        val cropped = Bitmap.createBitmap(full, left, top, w, h)

        // Scale to desired output size
        val out = cropped.scale(outputWidth.coerceAtLeast(1), outputHeight.coerceAtLeast(1))

        // Recycle intermediates to save memory
        if (cropped != out) cropped.recycle()
        full.recycle()

        return@withContext out
    }
}
