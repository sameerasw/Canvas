package com.sameerasw.canvas.data

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color

@Keep
data class TextItem(
    val id: Long = System.currentTimeMillis(),
    val x: Float,
    val y: Float,
    val text: String,
    val size: Float = 40f,
    val color: Color = Color.Black
)

