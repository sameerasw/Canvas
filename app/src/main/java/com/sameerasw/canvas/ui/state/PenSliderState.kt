package com.sameerasw.canvas.ui.state

data class PenSliderState(
    val penWidth: Float = 2.5f,
    val prevPenValue: Float = 2.5f,
    val smoothedStrength: Float = (2.5f - 1f) / (48f - 1f)
)

