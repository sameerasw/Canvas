package com.sameerasw.canvas.model

import androidx.annotation.Keep

@Keep
enum class ToolType {
    HAND, PEN, ERASER, TEXT, ARROW, SHAPE
}

@Keep
enum class PenStyle {
    NORMAL, SMOOTH, ROUGH
}

@Keep
enum class ShapeType {
    RECTANGLE, CIRCLE, TRIANGLE, LINE
}

