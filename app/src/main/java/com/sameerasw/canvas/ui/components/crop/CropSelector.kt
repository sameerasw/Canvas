package com.sameerasw.canvas.ui.components.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@Composable
fun CropSelector(
    initialRect: Rect,
    onCancel: () -> Unit,
    onConfirm: (Rect) -> Unit
) {
    // a draggable rectangle. Sizes are interpreted in pixels.
    val left = remember { mutableStateOf(initialRect.left) }
    val top = remember { mutableStateOf(initialRect.top) }
    val right = remember { mutableStateOf(initialRect.right) }
    val bottom = remember { mutableStateOf(initialRect.bottom) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Draggable rect
        Box(
            modifier = Modifier
                .offset { IntOffset(left.value.toInt(), top.value.toInt()) }
                .size((right.value - left.value).dp, (bottom.value - top.value).dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        left.value += dragAmount.x
                        right.value += dragAmount.x
                        top.value += dragAmount.y
                        bottom.value += dragAmount.y
                    }
                }
                .background(Color(0x55FFFFFF), RoundedCornerShape(8.dp))
        ) {}

        // Confirm/Cancel
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Button(onClick = { onConfirm(Rect(left.value, top.value, right.value, bottom.value)) }, modifier = Modifier.offset { IntOffset(0, -40) }) {
                Text("Confirm")
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.offset { IntOffset(140, -40) }) {
                Text("Cancel")
            }
        }
    }
}
