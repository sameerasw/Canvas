package com.sameerasw.canvas.ui.state

import androidx.compose.ui.geometry.Offset
import com.sameerasw.canvas.model.ToolType

data class CanvasUiState(
    val currentTool: ToolType = ToolType.PEN,
    val expanded: Boolean = false,
    val topMenuOpen: Boolean = false,
    val penWidth: Float = 2.5f,
    val textSize: Float = 16f,
    val showPenOptions: Boolean = false,
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val showTextDialog: Boolean = false,
    val showTextOptions: Boolean = false,
    val pendingTextPosition: Offset = Offset.Zero,
    val pendingTextValue: String = "",
    val selectedTextId: Long? = null,
    val isMovingText: Boolean = false
)

