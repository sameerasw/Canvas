package com.sameerasw.canvas.ui.state

data class TextSliderState(
    val textSize: Float = 16f,
    val prevTextValue: Float = 16f,
    val smoothedTextStrength: Float = (16f - 8f) / (128f - 8f)
)

