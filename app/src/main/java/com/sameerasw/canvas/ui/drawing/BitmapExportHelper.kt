package com.sameerasw.canvas.ui.drawing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.canvas.R
import com.sameerasw.canvas.model.DrawStroke
import com.sameerasw.canvas.data.TextItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BitmapExportHelper {
    suspend fun createBitmapFromData(
        context: Context,
        strokes: List<DrawStroke>,
        texts: List<TextItem>,
        outputWidth: Int = 2048,
        outputHeight: Int = 2048
    ): Bitmap? = withContext(Dispatchers.Default) {
        if (strokes.isEmpty() && texts.isEmpty()) return@withContext null

        val bmp = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
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
}

