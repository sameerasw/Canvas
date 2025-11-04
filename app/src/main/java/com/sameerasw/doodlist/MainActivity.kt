package com.sameerasw.doodlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.zIndex
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.sameerasw.doodlist.ui.theme.DoodListTheme
import kotlin.math.abs
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
enum class ToolType {
    HAND, PEN, ERASER
}

data class DrawStroke(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val width: Float = 2.5f
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoodListTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CanvasApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CanvasApp() {
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas area - bottom layer (z-index: 0)
        DrawingCanvas(
            currentTool = currentTool,
            strokes = strokes,
            modifier = Modifier.fillMaxSize()
        )

        // HorizontalFloatingToolbar at bottom center - top layer overlay
        HorizontalFloatingToolbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                .zIndex(1f),
            expanded = expanded,
            content = {
                // Center button - shows selected tool when collapsed, gesture when expanded
                FilledIconButton(
                    modifier = Modifier.width(if (expanded) 72.dp else 64.dp),
                    onClick = {
                        expanded = !expanded
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (expanded) {
                                R.drawable.rounded_gesture_24
                            } else {
                                when (currentTool) {
                                    ToolType.HAND -> R.drawable.rounded_back_hand_24
                                    ToolType.PEN -> R.drawable.rounded_stylus_fountain_pen_24
                                    ToolType.ERASER -> R.drawable.rounded_ink_eraser_24
                                }
                            }
                        ),
                        contentDescription = "Toggle toolbar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.width(if (expanded) 32.dp else 24.dp)
                    )
                }
            },
            trailingContent = {
                // Hand tool
                IconButton(
                    modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                    onClick = {
                        currentTool = ToolType.HAND
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_back_hand_24),
                        contentDescription = "Hand tool",
                        tint = if (currentTool == ToolType.HAND)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (expanded) 28.dp else 24.dp)
                    )
                }

                // Pen tool
                IconButton(
                    modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                    onClick = {
                        currentTool = ToolType.PEN
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_stylus_fountain_pen_24),
                        contentDescription = "Pen tool",
                        tint = if (currentTool == ToolType.PEN)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (expanded) 28.dp else 24.dp)
                    )
                }

                // Eraser tool
                IconButton(
                    modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                    onClick = {
                        currentTool = ToolType.ERASER
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_ink_eraser_24),
                        contentDescription = "Eraser tool",
                        tint = if (currentTool == ToolType.ERASER)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (expanded) 28.dp else 24.dp)
                    )
                }
            }
        )
    }
}

@Composable
fun DrawingCanvas(
    currentTool: ToolType,
    strokes: MutableList<DrawStroke>,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val currentStroke = remember { mutableStateListOf<Offset>() }
    val strokeColor = MaterialTheme.colorScheme.onBackground
    val eraserRadius = 30f

    Canvas(
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .pointerInput(currentTool) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Always allow two-finger zoom and pan
                    scale = (scale * zoom).coerceIn(0.5f, 3f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
            .pointerInput(currentTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke.clear()
                        currentStroke.add(offset)
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        currentStroke.add(change.position)
                        change.consume()

                        when (currentTool) {
                            ToolType.HAND -> {
                                // Hand mode: move canvas with one finger
                                offsetX += change.position.x - change.previousPosition.x
                                offsetY += change.position.y - change.previousPosition.y
                            }
                            ToolType.PEN -> {
                                // Pen mode: draw continuously (visual feedback in canvas)
                            }
                            ToolType.ERASER -> {
                                // Eraser mode: remove strokes at position
                                strokes.removeAll { stroke ->
                                    stroke.points.any { point ->
                                        val distance = kotlin.math.hypot(
                                            change.position.x - point.x,
                                            change.position.y - point.y
                                        )
                                        distance < eraserRadius
                                    }
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        if (currentTool == ToolType.PEN && currentStroke.size >= 2) {
                            strokes.add(DrawStroke(currentStroke.toList(), strokeColor))
                        }
                        currentStroke.clear()
                    },
                    onDragCancel = {
                        currentStroke.clear()
                    }
                )
            }
    ) {
        // Draw all strokes
        strokes.forEach { stroke ->
            drawScribbleStroke(stroke.points, stroke.color)
        }

        // Draw current stroke being drawn
        if (currentTool == ToolType.PEN && currentStroke.size >= 2) {
            drawScribbleStroke(currentStroke.toList(), strokeColor)
        }

        // Draw eraser preview circle
        if (currentTool == ToolType.ERASER && currentStroke.isNotEmpty()) {
            drawCircle(
                color = strokeColor.copy(alpha = 0.3f),
                radius = eraserRadius,
                center = currentStroke.last()
            )
        }
    }
}

private fun DrawScope.drawScribbleStroke(stroke: List<Offset>, color: Color) {
    if (stroke.size < 2) return

    val random = Random(42)
    val scribbleOffsets = listOf(-1.2f, -0.6f, 0f, 0.6f, 1.2f)
    val baseWidth = 2.5f

    scribbleOffsets.forEachIndexed { offsetIndex, scribbleOffset ->
        val path = Path().apply {
            moveTo(stroke.first().x + scribbleOffset, stroke.first().y + scribbleOffset)

            for (i in 1 until stroke.size) {
                val current = stroke[i]
                val randomOffsetX = (random.nextFloat() - 0.5f) * 1.5f * (6 - abs(offsetIndex - 2))
                val randomOffsetY = (random.nextFloat() - 0.5f) * 1.5f * (6 - abs(offsetIndex - 2))

                val scribbledX = current.x + scribbleOffset + randomOffsetX
                val scribbledY = current.y + scribbleOffset + randomOffsetY

                lineTo(scribbledX, scribbledY)
            }
        }

        val widthFactor = 1f - (abs(offsetIndex - 2) * 0.15f)
        val strokeWidth = baseWidth * widthFactor

        drawPath(path, color.copy(alpha = 0.65f + 0.35f * widthFactor), style = Stroke(width = strokeWidth))
    }

    val mainPath = Path().apply {
        moveTo(stroke.first().x, stroke.first().y)
        for (i in 1 until stroke.size) {
            lineTo(stroke[i].x, stroke[i].y)
        }
    }
    drawPath(mainPath, color, style = Stroke(width = 1.5f))
}

