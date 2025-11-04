package com.sameerasw.doodlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
enum class ToolType {
    HAND, PEN, ERASER, TEXT
}

data class DrawStroke(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val width: Float = 2.5f
)

class MainActivity : ComponentActivity() {
    private val viewModel: CanvasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoodListTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    CanvasApp(viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.save()
    }

    override fun onStop() {
        super.onStop()
        viewModel.save()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CanvasApp(viewModel: CanvasViewModel) {
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    val strokes by viewModel.strokes.collectAsState()
    val texts by viewModel.texts.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas area - bottom layer (z-index: 0)
        DrawingCanvas(
            currentTool = currentTool,
            strokes = strokes.toMutableList(),
            texts = texts,
            onAddStroke = { viewModel.addStroke(it) },
            onRemoveStroke = { predicate -> viewModel.removeStroke(predicate) },
            onAddText = { viewModel.addText(it) },
            onUpdateText = { viewModel.updateText(it) },
            onRemoveText = { viewModel.removeText(it) },
            modifier = Modifier.fillMaxSize()
        )

        // HorizontalFloatingToolbar at bottom center - top layer overlay
        HorizontalFloatingToolbar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                .zIndex(1f),
            expanded = expanded,
            // two items on the left of the center
            leadingContent = {
                // Hand tool
                IconButton(
                    modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                    onClick = { currentTool = ToolType.HAND }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_back_hand_24),
                        contentDescription = "Hand tool",
                        tint = if (currentTool == ToolType.HAND) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (expanded) 28.dp else 24.dp)
                    )
                }

                // Pen tool
                IconButton(
                    modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                    onClick = { currentTool = ToolType.PEN }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_stylus_fountain_pen_24),
                        contentDescription = "Pen tool",
                        tint = if (currentTool == ToolType.PEN) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (expanded) 28.dp else 24.dp)
                    )
                }
            },
            // center content remains the primary button
            content = {
                FilledIconButton(
                    modifier = Modifier.width(if (expanded) 72.dp else 64.dp),
                    onClick = { expanded = !expanded }
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (expanded) R.drawable.rounded_gesture_24 else when (currentTool) {
                                ToolType.HAND -> R.drawable.rounded_back_hand_24
                                ToolType.PEN -> R.drawable.rounded_stylus_fountain_pen_24
                                ToolType.ERASER -> R.drawable.rounded_ink_eraser_24
                                ToolType.TEXT -> R.drawable.rounded_text_fields_24
                            }
                        ),
                        contentDescription = "Toggle toolbar",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.width(if (expanded) 32.dp else 24.dp)
                    )
                }
            },
            // two items on the right of the center
            trailingContent = {
                // Eraser tool
                IconButton(
                    modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                    onClick = { currentTool = ToolType.ERASER }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_ink_eraser_24),
                        contentDescription = "Eraser tool",
                        tint = if (currentTool == ToolType.ERASER) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(if (expanded) 28.dp else 24.dp)
                    )
                }

                // Text tool
                IconButton(
                    modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                    onClick = { currentTool = ToolType.TEXT }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_text_fields_24),
                        contentDescription = "Text tool",
                        tint = if (currentTool == ToolType.TEXT) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
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
    texts: List<com.sameerasw.doodlist.data.TextItem> = emptyList(),
    modifier: Modifier = Modifier,
    onAddStroke: ((DrawStroke) -> Unit)? = null,
    onRemoveStroke: ((predicate: (DrawStroke) -> Boolean) -> Unit)? = null,
    onAddText: ((com.sameerasw.doodlist.data.TextItem) -> Unit)? = null,
    onUpdateText: ((com.sameerasw.doodlist.data.TextItem) -> Unit)? = null,
    onRemoveText: ((Long) -> Unit)? = null
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val currentStroke = remember { mutableStateListOf<Offset>() }
    val strokeColor = MaterialTheme.colorScheme.onBackground
    val eraserRadius = 30f

    val context = LocalContext.current
    val themeColor = MaterialTheme.colorScheme.onBackground.toArgb()

    // Text dialog state
    var showTextDialog by remember { mutableStateOf(false) }
    var showTextOptions by remember { mutableStateOf(false) }
    var pendingTextPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingTextValue by remember { mutableStateOf("") }

    // Selected text for context menu / moving
    var selectedTextId by remember { mutableStateOf<Long?>(null) }
    var isMovingText by remember { mutableStateOf(false) }

    Canvas(
        modifier = modifier
            // removed graphicsLayer to avoid double-transform issues
            .pointerInput(currentTool) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // Adjust scale around the gesture centroid so zoom feels natural
                    val prevScale = scale
                    val newScale = (scale * zoom).coerceIn(0.5f, 3f)

                    // Compute centroid in world coords before scaling
                    val worldCx = (centroid.x - offsetX) / prevScale
                    val worldCy = (centroid.y - offsetY) / prevScale

                    // Apply new scale
                    scale = newScale

                    // Recompute offset so the centroid screen position remains under the fingers
                    // Do NOT add pan.x/pan.y here: centroid already reflects finger movement.
                    offsetX = centroid.x - worldCx * scale
                    offsetY = centroid.y - worldCy * scale
                }
            }
            .pointerInput(currentTool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke.clear()
                        // convert to world coords
                        val worldStart = Offset((offset.x - offsetX) / scale, (offset.y - offsetY) / scale)
                        currentStroke.add(worldStart)

                        if (currentTool == ToolType.TEXT) {
                            // store pending position in world coords
                            pendingTextPosition = worldStart
                        }
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        // If moving text
                        if (isMovingText && selectedTextId != null) {
                            val worldPos = Offset((change.position.x - offsetX) / scale, (change.position.y - offsetY) / scale)
                            onUpdateText?.invoke(
                                com.sameerasw.doodlist.data.TextItem(
                                    id = selectedTextId!!,
                                    x = worldPos.x,
                                    y = worldPos.y,
                                    text = texts.first { it.id == selectedTextId!! }.text
                                )
                            )
                            change.consume()
                            return@detectDragGestures
                        }

                        val worldPos = Offset((change.position.x - offsetX) / scale, (change.position.y - offsetY) / scale)
                        currentStroke.add(worldPos)
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
                                // Eraser mode: remove strokes at position (compare in world coords)
                                val worldThreshold = eraserRadius / scale
                                onRemoveStroke?.invoke { stroke ->
                                    stroke.points.any { point ->
                                        val distance = kotlin.math.hypot(
                                            worldPos.x - point.x,
                                            worldPos.y - point.y
                                        )
                                        distance < worldThreshold
                                    }
                                }

                                // Also remove text items within threshold (treat text like drawable strokes)
                                onRemoveText?.let { removeText ->
                                    // find text ids close enough to the eraser
                                    val toRemove = texts.filter { text ->
                                        val dx = worldPos.x - text.x
                                        val dy = worldPos.y - text.y
                                        val distance = kotlin.math.hypot(dx, dy)
                                        // consider text size as extra radius (half of font size as rough bounding)
                                        distance < worldThreshold + (text.size * 0.5f)
                                    }.map { it.id }

                                    toRemove.forEach { id -> removeText(id) }
                                }
                            }
                            ToolType.TEXT -> {
                                // Text mode: dragging shouldn't draw; we use long-press to add
                            }
                        }
                    },
                    onDragEnd = {
                        if (currentTool == ToolType.PEN && currentStroke.size >= 2) {
                            val newStroke = DrawStroke(currentStroke.toList(), strokeColor)
                            onAddStroke?.invoke(newStroke)
                        }
                        currentStroke.clear()
                        // finish moving
                        isMovingText = false
                        selectedTextId = null
                    },
                    onDragCancel = {
                        currentStroke.clear()
                        isMovingText = false
                        selectedTextId = null
                    }
                )
            }
            .pointerInput(currentTool) {
                // detectTapGestures to support longPress to add text and tapping existing text for options
                detectTapGestures(
                    onLongPress = { offset ->
                        if (currentTool == ToolType.TEXT) {
                            // convert to world coords for hit-test
                            val world = Offset((offset.x - offsetX) / scale, (offset.y - offsetY) / scale)

                            val hit = texts.find { text ->
                                val half = text.size
                                world.x >= text.x - half && world.x <= text.x + half &&
                                        world.y >= text.y - half && world.y <= text.y + half
                            }

                            if (hit != null) {
                                // long press on existing text -> show options (edit/move/delete)
                                selectedTextId = hit.id
                                pendingTextValue = hit.text
                                pendingTextPosition = Offset(hit.x, hit.y)
                                showTextOptions = true
                            } else {
                                // add new text at pointer location (store world coords)
                                pendingTextPosition = world
                                pendingTextValue = ""
                                showTextDialog = true
                            }
                        }
                    },
                    onTap = { offset ->
                        // Tapping an existing text in TEXT mode starts move
                        if (currentTool == ToolType.TEXT) {
                            val world = Offset((offset.x - offsetX) / scale, (offset.y - offsetY) / scale)
                            val hit = texts.find { text ->
                                val half = text.size
                                world.x >= text.x - half && world.x <= text.x + half &&
                                        world.y >= text.y - half && world.y <= text.y + half
                            }
                            if (hit != null) {
                                selectedTextId = hit.id
                                isMovingText = true
                            }
                        }
                    }
                )
            }
    ) {
        // Draw by mapping world coordinates (strokes/texts) -> screen coordinates explicitly.
        // This avoids transform mismatch between pointer input and drawing.
        // Draw all strokes (stored in world coords) transformed to screen coords
        strokes.forEach { stroke ->
            val screenPoints = stroke.points.map { world -> Offset(world.x * scale + offsetX, world.y * scale + offsetY) }
            if (screenPoints.size >= 2) drawScribbleStroke(screenPoints, strokeColor)
        }

        // Draw texts at their world positions (map to screen and scale font size)
        texts.forEach { t ->
            val sx = t.x * scale + offsetX
            val sy = t.y * scale + offsetY
            val fontSize = t.size * scale
            drawStringWithFont(context, t.text, sx, sy, fontSize, themeColor)
        }

        // Draw current stroke being drawn (world coords -> screen coords)
        if (currentTool == ToolType.PEN && currentStroke.size >= 2) {
            val screenPoints = currentStroke.map { world -> Offset(world.x * scale + offsetX, world.y * scale + offsetY) }
            drawScribbleStroke(screenPoints, strokeColor)
        }

        // Draw eraser preview circle in screen coords
        if (currentTool == ToolType.ERASER && currentStroke.isNotEmpty()) {
            val last = currentStroke.last()
            val center = Offset(last.x * scale + offsetX, last.y * scale + offsetY)
            drawCircle(
                color = strokeColor.copy(alpha = 0.3f),
                radius = eraserRadius,
                center = center
            )
        }
    }

    // Show Text options dialog (edit/move/delete) for existing text
    if (showTextOptions && selectedTextId != null) {
        AlertDialog(
            onDismissRequest = { showTextOptions = false; selectedTextId = null },
            title = { Text("Text options") },
            text = { Text("Choose an action for the selected text") },
            confirmButton = {
                TextButton(onClick = {
                    // Edit
                    val hit = texts.firstOrNull { it.id == selectedTextId }
                    if (hit != null) {
                        pendingTextValue = hit.text
                        pendingTextPosition = Offset(hit.x, hit.y)
                        showTextDialog = true
                    }
                    showTextOptions = false
                }) { Text("Edit") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        // Move
                        isMovingText = true
                        showTextOptions = false
                    }) { Text("Move") }

                    TextButton(onClick = {
                        // Delete
                        onRemoveText?.invoke(selectedTextId!!)
                        selectedTextId = null
                        showTextOptions = false
                    }) { Text("Delete") }

                    TextButton(onClick = {
                        selectedTextId = null
                        showTextOptions = false
                    }) { Text("Cancel") }
                }
            }
        )
    }

    // Show Text add/edit dialog
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false; selectedTextId = null },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedTextId != null) {
                        // editing existing
                        onUpdateText?.invoke(
                            com.sameerasw.doodlist.data.TextItem(
                                id = selectedTextId!!,
                                x = pendingTextPosition.x,
                                y = pendingTextPosition.y,
                                text = pendingTextValue
                            )
                        )
                    } else {
                        // adding new
                        onAddText?.invoke(
                            com.sameerasw.doodlist.data.TextItem(
                                x = pendingTextPosition.x,
                                y = pendingTextPosition.y,
                                text = pendingTextValue
                            )
                        )
                    }
                    showTextDialog = false
                    selectedTextId = null
                }) {
                    Text(if (selectedTextId != null) "Save" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTextDialog = false
                    selectedTextId = null
                }) {
                    Text("Cancel")
                }
            },
            title = { Text(if (selectedTextId != null) "Edit Text" else "Add Text") },
            text = {
                OutlinedTextField(
                    value = pendingTextValue,
                    onValueChange = { pendingTextValue = it },
                    label = { Text("Text") }
                )
            }
        )
    }
}

// helper function to draw text with the custom font
private fun DrawScope.drawStringWithFont(context: android.content.Context, text: String, x: Float, y: Float, fontSize: Float, colorInt: Int) {
    if (text.isEmpty()) return
    val size = if (fontSize <= 0f) 16f else fontSize
    // DrawScope doesn't provide direct font loading here; we use native drawText via drawIntoCanvas
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = colorInt
            textSize = size
            isAntiAlias = true
            // load font from resources
            try {
                val tf = ResourcesCompat.getFont(context, com.sameerasw.doodlist.R.font.font)
                if (tf != null) typeface = tf
            } catch (e: Exception) {
                // ignore and use default
            }
        }
        // adjust baseline so x,y is top-left of text
        val fm = paint.fontMetrics
        val baseline = y - fm.ascent
        canvas.nativeCanvas.drawText(text, x, baseline, paint)
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
