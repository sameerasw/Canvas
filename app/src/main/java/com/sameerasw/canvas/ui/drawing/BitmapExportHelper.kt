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
            paint.color = android.graphics.Color.BLACK
            paint.strokeWidth = s.width

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

        val textPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = android.graphics.Color.BLACK
        }

        texts.forEach { t ->
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
            paint.color = android.graphics.Color.BLACK
            // scale stroke width by transformScale
            paint.strokeWidth = s.width * transformScale

            val path = AndroidPath()
            val pts = s.points
            // Convert world coordinates -> view pixels: x * scale + offsetX
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

        val textPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = android.graphics.Color.BLACK
        }

        texts.forEach { t ->
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
