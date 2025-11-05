package com.sameerasw.canvas.ui.drawing

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sameerasw.canvas.SettingsRepository

/**
 * Utility object for drawing canvas background patterns.
 */
object BackgroundDrawer {
    private const val DOT_RADIUS = 3f
    private const val DOT_SPACING = 50f  // Increased from 24f
    private const val LINE_SPACING = 50f  // Increased from 24f
    private const val LINE_WIDTH = 1f

    /**
     * Draw background pattern on a Compose DrawScope (for live canvas) with transform support.
     * The pattern moves with the canvas at 1:1 ratio.
     */
    fun drawBackgroundOnCompose(
        drawScope: DrawScope,
        backgroundType: SettingsRepository.CanvasBackgroundType,
        width: Float,
        height: Float,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        when (backgroundType) {
            SettingsRepository.CanvasBackgroundType.DOTS -> {
                drawDotsWithTransform(drawScope, width, height, scale, offsetX, offsetY)
            }
            SettingsRepository.CanvasBackgroundType.LINES -> {
                drawLinesWithTransform(drawScope, width, height, scale, offsetX, offsetY)
            }
            SettingsRepository.CanvasBackgroundType.NONE -> {
                // No background
            }
        }
    }

    /**
     * Draw background pattern on an Android Canvas (for bitmap export).
     * Extended to cover a larger area for continuous patterns.
     */
    fun drawBackgroundOnAndroidCanvas(
        canvas: Canvas,
        backgroundType: SettingsRepository.CanvasBackgroundType,
        width: Int,
        height: Int,
        lineColor: Int = android.graphics.Color.BLACK,
        extendFactor: Float = 1.5f  // Extend background by this factor
    ) {
        when (backgroundType) {
            SettingsRepository.CanvasBackgroundType.DOTS -> {
                drawDotsOnAndroidCanvas(canvas, width, height, lineColor, extendFactor)
            }
            SettingsRepository.CanvasBackgroundType.LINES -> {
                drawLinesOnAndroidCanvas(canvas, width, height, lineColor, extendFactor)
            }
            SettingsRepository.CanvasBackgroundType.NONE -> {
                // No background
            }
        }
    }

    private fun drawDotsWithTransform(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val gridColor = Color.Gray.copy(alpha = 0.3f)

        // Convert view coordinates to world coordinates for grid alignment
        val startX = -offsetX / scale
        val startY = -offsetY / scale
        val endX = startX + width / scale
        val endY = startY + height / scale

        // Calculate grid start positions
        val gridStartX = (kotlin.math.floor(startX / DOT_SPACING) * DOT_SPACING)
        val gridStartY = (kotlin.math.floor(startY / DOT_SPACING) * DOT_SPACING)

        var worldY = gridStartY
        while (worldY <= endY) {
            var worldX = gridStartX
            while (worldX <= endX) {
                val screenX = worldX * scale + offsetX
                val screenY = worldY * scale + offsetY

                if (screenX >= -DOT_RADIUS && screenX <= width + DOT_RADIUS &&
                    screenY >= -DOT_RADIUS && screenY <= height + DOT_RADIUS) {
                    drawScope.drawCircle(
                        color = gridColor,
                        radius = DOT_RADIUS,
                        center = androidx.compose.ui.geometry.Offset(screenX, screenY)
                    )
                }
                worldX += DOT_SPACING
            }
            worldY += DOT_SPACING
        }
    }

    private fun drawLinesWithTransform(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val lineColor = Color.Gray.copy(alpha = 0.3f)

        // Convert view coordinates to world coordinates for grid alignment
        val startY = -offsetY / scale
        val endY = startY + height / scale

        // Calculate grid start position
        val gridStartY = (kotlin.math.floor(startY / LINE_SPACING) * LINE_SPACING)

        var worldY = gridStartY
        while (worldY <= endY) {
            val screenY = worldY * scale + offsetY

            if (screenY >= -LINE_WIDTH && screenY <= height + LINE_WIDTH) {
                drawScope.drawLine(
                    color = lineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, screenY),
                    end = androidx.compose.ui.geometry.Offset(width, screenY),
                    strokeWidth = LINE_WIDTH
                )
            }
            worldY += LINE_SPACING
        }
    }

    private fun drawDotsOnAndroidCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        lineColor: Int,
        extendFactor: Float = 1.5f
    ) {
        val paint = Paint().apply {
            color = lineColor
            alpha = (255 * 0.3f).toInt()
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Extend the background area
        val extendedWidth = (width * extendFactor).toInt()
        val extendedHeight = (height * extendFactor).toInt()
        val offsetX = ((extendedWidth - width) / 2f).toInt()
        val offsetY = ((extendedHeight - height) / 2f).toInt()

        var y = -offsetY.toFloat()
        while (y < height + offsetY) {
            var x = -offsetX.toFloat()
            while (x < width + offsetX) {
                canvas.drawCircle(x, y, DOT_RADIUS, paint)
                x += DOT_SPACING
            }
            y += DOT_SPACING
        }
    }

    private fun drawLinesOnAndroidCanvas(
        canvas: Canvas,
        width: Int,
        height: Int,
        lineColor: Int,
        extendFactor: Float = 1.5f
    ) {
        val paint = Paint().apply {
            color = lineColor
            alpha = (255 * 0.3f).toInt()
            isAntiAlias = true
            strokeWidth = LINE_WIDTH
            style = Paint.Style.STROKE
        }

        // Extend the background area
        val extendedHeight = (height * extendFactor).toInt()
        val offsetY = ((extendedHeight - height) / 2f).toInt()

        var y = -offsetY.toFloat()
        while (y < height + offsetY) {
            canvas.drawLine(0f, y, width.toFloat(), y, paint)
            y += LINE_SPACING
        }
    }
}

