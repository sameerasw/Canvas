package com.sameerasw.canvas.ui.drawing

import android.content.Context
import android.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.core.content.res.ResourcesCompat
import com.sameerasw.canvas.R

object TextDrawer {
    fun DrawScope.drawStringWithFont(context: Context, text: String, x: Float, y: Float, fontSize: Float, colorInt: Int) {
        if (text.isEmpty()) return
        val size = if (fontSize <= 0f) 16f else fontSize

        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = colorInt
                textSize = size
                isAntiAlias = true
                try {
                    val tf = ResourcesCompat.getFont(context, R.font.font)
                    if (tf != null) typeface = tf
                } catch (_: Exception) {
                    // ignore and use default
                }
            }
            val fm = paint.fontMetrics
            val baseline = y - fm.ascent
            canvas.nativeCanvas.drawText(text, x, baseline, paint)
        }
    }
}

