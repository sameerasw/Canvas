package com.sameerasw.canvas.ui.state

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job

data class DrawingCanvasState(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val lastMoveTime: Long = 0L,
    val lastMovePos: Offset = Offset.Zero,
    val currentSpeed: Float = 0f,
    val drawingHapticJob: Job? = null,
    val selectedTextId: Long? = null,
    val isMovingText: Boolean = false
)

