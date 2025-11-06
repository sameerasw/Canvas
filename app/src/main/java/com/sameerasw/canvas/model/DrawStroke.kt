package com.sameerasw.canvas.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

data class StylusPoint(
    val offset: Offset,
    val pressure: Float = 1f,
    val tilt: Float = 0f,
    val orientation: Float = 0f
)

data class DrawStroke(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val width: Float = 2.5f,
    val style: PenStyle = PenStyle.NORMAL,
    val isArrow: Boolean = false,
    val shapeType: ShapeType? = null,
    val isFilled: Boolean = false,
    val stylusPoints: List<StylusPoint>? = null,
    val isFromStylus: Boolean = false
)

