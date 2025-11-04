package com.sameerasw.doodlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.LaunchedEffect

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

    // Pen options UI state: width and whether options are visible
    var penWidth by remember { mutableStateOf(2.5f) }
    var showPenOptions by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas area - bottom layer (z-index: 0)
        DrawingCanvas(
            currentTool = currentTool,
            strokes = strokes.toMutableList(),
            texts = texts,
            penWidth = penWidth,
            onAddStroke = { viewModel.addStroke(it) },
            onRemoveStroke = { predicate -> viewModel.removeStroke(predicate) },
            onAddText = { viewModel.addText(it) },
            onUpdateText = { viewModel.updateText(it) },
            onRemoveText = { viewModel.removeText(it) },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom column that contains the secondary pen options and the main floating toolbar.
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp) // offset slightly from system gesture/navigation bar
                .zIndex(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Secondary pen options toolbar (appears when pen icon tapped again)
            AnimatedVisibility(
                visible = showPenOptions,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(260)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220)),
            ) {
                Surface(
                    modifier = Modifier,
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .width(280.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // live preview circle centered above the slider
                        val circleSize = (penWidth * 1.5f).dp.coerceAtLeast(10.dp).coerceAtMost(64.dp)
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.width(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(circleSize)
                            ) {}
                        }

                        // Slider to pick pen width
                        androidx.compose.material3.Slider(
                            value = penWidth,
                            onValueChange = { penWidth = it },
                            valueRange = 1f..48f,
                            modifier = Modifier.width(240.dp)
                        )
                    }
                }
            }

            // Small spacer between secondary toolbar and main toolbar
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(6.dp))

            // HorizontalFloatingToolbar at bottom center - top layer overlay
            HorizontalFloatingToolbar(
                modifier = Modifier
                    .zIndex(1f),
                 expanded = expanded,
                 // two items on the left of the center
                 leadingContent = {
                     // Hand tool
                     IconButton(
                         modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                         onClick = {
                             currentTool = ToolType.HAND
                             showPenOptions = false
                         }
                     ) {
                         Icon(
                             painter = painterResource(id = R.drawable.rounded_back_hand_24),
                             contentDescription = "Hand tool",
                             tint = if (currentTool == ToolType.HAND) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.width(if (expanded) 28.dp else 24.dp)
                         )
                     }

                     // Pen tool: if tapped while already selected, toggle pen options
                     IconButton(
                         modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                         onClick = {
                             if (currentTool == ToolType.PEN) {
                                 showPenOptions = !showPenOptions
                             } else {
                                 currentTool = ToolType.PEN
                                 showPenOptions = false
                             }
                         }
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
                         onClick = {
                             val new = !expanded
                             expanded = new
                             if (!new) showPenOptions = false
                         }
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
                         onClick = {
                             currentTool = ToolType.ERASER
                             showPenOptions = false
                         }
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
                         onClick = {
                             currentTool = ToolType.TEXT
                             showPenOptions = false
                         }
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
}

@Composable
fun DrawingCanvas(
    currentTool: ToolType,
    strokes: MutableList<DrawStroke>,
    texts: List<com.sameerasw.doodlist.data.TextItem> = emptyList(),
    penWidth: Float = 2.5f,
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
                    // Make pinch easier: amplify the gesture zoom slightly so small finger movement scales more
                    val zoomSensitivity = 1.6f // >1 => easier to zoom
                    val effectiveZoom = 1f + (zoom - 1f) * zoomSensitivity

                    // Adjust scale around the gesture centroid so zoom feels natural
                    val prevScale = scale
                    val newScale = (scale * effectiveZoom).coerceIn(0.25f, 6f)

                    // Compute centroid in world coords before scaling
                    val worldCx = (centroid.x - offsetX) / prevScale
                    val worldCy = (centroid.y - offsetY) / prevScale

                    // Apply new scale
                    scale = newScale

                    // Recompute offset so the centroid screen position remains under the fingers
                    offsetX = centroid.x - worldCx * scale
                    offsetY = centroid.y - worldCy * scale
                }
            }
            .pointerInput(currentTool, penWidth) {
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
                            // save stroke with the current penWidth (world units)
                            val newStroke = DrawStroke(currentStroke.toList(), strokeColor, width = penWidth)
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
                        val world = Offset((offset.x - offsetX) / scale, (offset.y - offsetY) / scale)

                        // If PEN tool: add a tiny dot stroke at tapped location
                        if (currentTool == ToolType.PEN) {
                            // create a very small two-point stroke so drawScribbleStroke can render it
                            val delta = 0.5f // world units (approx pixels at scale 1)
                            val p1 = world
                            val p2 = Offset(world.x + delta, world.y + delta)
                            onAddStroke?.invoke(DrawStroke(listOf(p1, p2), strokeColor, width = penWidth))
                            return@detectTapGestures
                        }

                        // Tapping an existing text in TEXT mode starts move
                        if (currentTool == ToolType.TEXT) {
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
            if (screenPoints.size >= 2) drawScribbleStroke(screenPoints, strokeColor, stroke.width * scale)
        }

        // Draw texts at their world positions (map to screen and scale font size)
        texts.forEach { t ->
            val sx = t.x * scale + offsetX
            val sy = t.y * scale + offsetY
            val fontSize = t.size * scale
            drawStringWithFont(context, t.text, sx, sy, fontSize, themeColor)
        }

        // Draw current stroke being drawn (world coords -> screen coords) using current penWidth so slider changes apply immediately
        if (currentTool == ToolType.PEN && currentStroke.size >= 2) {
            val screenPoints = currentStroke.map { world -> Offset(world.x * scale + offsetX, world.y * scale + offsetY) }
            drawScribbleStroke(screenPoints, strokeColor, penWidth * scale)
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
                val tf = ResourcesCompat.getFont(context, R.font.font)
                if (tf != null) typeface = tf
            } catch (_: Exception) {
                // ignore and use default
            }
        }
        // adjust baseline so x,y is top-left of text
        val fm = paint.fontMetrics
        val baseline = y - fm.ascent
        canvas.nativeCanvas.drawText(text, x, baseline, paint)
    }
}

private fun DrawScope.drawScribbleStroke(stroke: List<Offset>, color: Color, width: Float) {
    if (stroke.size < 2) return

    val baseWidth = width.coerceAtLeast(1.0f)

    // fewer, gentler scribble layers to reduce jaggedness
    val layerOffsets = listOf(-0.4f, 0f, 0.4f)

    // For each layer, build a smoothed path using quadratic bezier midpoints
    layerOffsets.forEachIndexed { offsetIndex, layerFactor ->
        val layerOffset = layerFactor * baseWidth
        val path = Path()
        // start
        path.moveTo(stroke.first().x + layerOffset, stroke.first().y + layerOffset)

        // create smoothed path: quadratic to midpoints
        for (i in 1 until stroke.size) {
            val prev = stroke[i - 1]
            val curr = stroke[i]
            val midX = (prev.x + curr.x) / 2f + layerOffset
            val midY = (prev.y + curr.y) / 2f + layerOffset
            path.quadraticTo(prev.x + layerOffset, prev.y + layerOffset, midX, midY)
        }
        // ensure last point
        path.lineTo(stroke.last().x + layerOffset, stroke.last().y + layerOffset)

        val widthFactor = 1f - (abs(offsetIndex - 1) * 0.25f)
        val strokeWidth = baseWidth * (0.6f + 0.4f * widthFactor)
        drawPath(path, color.copy(alpha = 0.55f + 0.25f * widthFactor), style = Stroke(width = strokeWidth))
    }

    // main smoothed path
    val mainPath = Path()
    mainPath.moveTo(stroke.first().x, stroke.first().y)
    for (i in 1 until stroke.size) {
        val prev = stroke[i - 1]
        val curr = stroke[i]
        val midX = (prev.x + curr.x) / 2f
        val midY = (prev.y + curr.y) / 2f
        mainPath.quadraticTo(prev.x, prev.y, midX, midY)
    }
    mainPath.lineTo(stroke.last().x, stroke.last().y)

    // Final main stroke uses baseWidth so finished strokes keep selected thickness
    drawPath(mainPath, color, style = Stroke(width = baseWidth))
}
