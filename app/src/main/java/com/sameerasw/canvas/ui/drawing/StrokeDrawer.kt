package com.sameerasw.canvas.ui.drawing

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

object StrokeDrawer {
    fun DrawScope.drawScribbleStroke(stroke: List<Offset>, color: Color, width: Float) {
        if (stroke.size < 2) return

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
}

