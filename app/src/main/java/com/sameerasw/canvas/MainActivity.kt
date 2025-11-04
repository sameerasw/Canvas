package com.sameerasw.canvas

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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalFloatingToolbar
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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.sameerasw.canvas.ui.theme.CanvasTheme
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sameerasw.canvas.utils.HapticUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.unit.DpOffset

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
            CanvasTheme {
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
    // top toolbar menu open state (visible only when bottom toolbar expanded)
    var topMenuOpen by remember { mutableStateOf(false) }

    // Pen options UI state: width and whether options are visible
    var penWidth by remember { mutableStateOf(2.5f) }
    // keep previous pen value to only tick on step changes
    var prevPenValue by remember { mutableStateOf(penWidth) }
    // smoothed normalized strength (0..1) for slider haptics
    var smoothedStrength by remember { mutableStateOf((penWidth - 1f) / (48f - 1f)) }

    // Text font size state: used when TEXT tool's secondary toolbar is shown
    var textSize by remember { mutableStateOf(16f) }
    var prevTextValue by remember { mutableStateOf(textSize) }
    var smoothedTextStrength by remember { mutableStateOf((textSize - 8f) / (128f - 8f)) }

    var showPenOptions by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        val haptics = LocalHapticFeedback.current
        // interaction source for top toolbar clickable so we don't create it during composition repeatedly
        val topInteractionSource = remember { MutableInteractionSource() }

        // Canvas area - bottom layer (z-index: 0)
        DrawingCanvas(
            currentTool = currentTool,
            strokes = strokes.toMutableList(),
            texts = texts,
            penWidth = penWidth,
            textSize = textSize,
            onAddStroke = { viewModel.addStroke(it) },
            onRemoveStroke = { predicate -> viewModel.removeStroke(predicate) },
            onAddText = { viewModel.addText(it) },
            onUpdateText = { viewModel.updateText(it) },
            onRemoveText = { viewModel.removeText(it) },
            modifier = Modifier.fillMaxSize()
        )

        // Top overlay toolbar: slides down from top when main toolbar is expanded
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(260)),
            exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(220)),
        ) {
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .zIndex(20f)
                    // add a no-op clickable to ensure this surface consumes pointer events before the Canvas
                    .clickable(interactionSource = topInteractionSource, indication = null) {},
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // left-corner pill container with Undo + Menu buttons
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp,
                            modifier = Modifier
                                .padding(start = 4.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Undo button: remove last stroke
                                IconButton(onClick = {
                                    if (strokes.isNotEmpty()) {
                                        viewModel.setStrokes(strokes.dropLast(1))
                                    }
                                }) {
                                    Icon(painter = painterResource(id = R.drawable.rounded_undo_24), contentDescription = "Undo", tint = MaterialTheme.colorScheme.onSurface)
                                }

                                // Menu toggle button
                                IconButton(onClick = { topMenuOpen = !topMenuOpen }, modifier = Modifier.size(40.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_keyboard_arrow_down_24),
                                        contentDescription = "Top menu",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                // DropdownMenu anchored to the menu button — shows horizontal icon row
                                DropdownMenu(
                                    expanded = topMenuOpen,
                                    onDismissRequest = { topMenuOpen = false },
                                    modifier = Modifier
                                        .wrapContentWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    offset = DpOffset(x = 0.dp, y = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { topMenuOpen = false }) {
                                                Icon(painter = painterResource(id = R.drawable.rounded_ios_share_24), contentDescription = "Share")
                                            }
                                            IconButton(onClick = { topMenuOpen = false }) {
                                                Icon(painter = painterResource(id = R.drawable.rounded_download_24), contentDescription = "Save")
                                            }
                                            IconButton(onClick = { topMenuOpen = false }) {
                                                Icon(painter = painterResource(id = R.drawable.rounded_cleaning_services_24), contentDescription = "Clear all")
                                            }
                                            IconButton(onClick = { topMenuOpen = false }) {
                                                Icon(painter = painterResource(id = R.drawable.rounded_settings_24), contentDescription = "Settings")
                                            }
                                            IconButton(onClick = { topMenuOpen = false }) {
                                                Icon(painter = painterResource(id = R.drawable.rounded_info_24), contentDescription = "About")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // keep left alignment; no spacer needed
                    }
                 }
            }
        }

        // Bottom column that contains the secondary pen options and the main floating toolbar.
        Column(
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
                    Column(
                         modifier = Modifier
                             .padding(12.dp)
                             .width(280.dp),
                         horizontalAlignment = Alignment.CenterHorizontally
                     ) {
                        // When the Pen tool is active we show the circle preview + pen slider.
                        // When the Text tool is active we reuse this same area to show a font-size preview and slider.
                        if (currentTool == ToolType.PEN) {
                            // live preview circle centered above the slider
                            val circleSize = (penWidth * 1.5f).dp.coerceAtLeast(10.dp).coerceAtMost(64.dp)
                            Box(
                                modifier = Modifier.width(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(circleSize)
                                ) {}
                            }

                            // Slider to pick pen width - call variable haptic on value changes
                            androidx.compose.material3.Slider(
                                value = penWidth,
                                onValueChange = { new ->
                                    // Only trigger a tick when the integer step changes to avoid excessive ticks
                                    val prevStep = prevPenValue.toInt()
                                    val newStep = new.toInt()
                                    penWidth = new
                                    if (newStep != prevStep) {
                                        prevPenValue = new
                                        // normalize strength 0..1 across the slider range
                                        val strength = (new - 1f) / (48f - 1f)
                                        // smooth the strength using EMA so ticks feel gradual
                                        val alpha = 0.35f
                                        smoothedStrength = smoothedStrength * (1f - alpha) + strength * alpha
                                        HapticUtil.performVariableTick(haptics, smoothedStrength)
                                    }
                                },
                                onValueChangeFinished = {
                                    // strong confirm tick when lifting
                                    HapticUtil.performClick(haptics)
                                },
                                valueRange = 1f..48f,
                                modifier = Modifier.width(240.dp)
                            )
                        } else if (currentTool == ToolType.TEXT) {
                            // font-size preview (show letter "A" scaled to chosen size)
                            Box(
                                modifier = Modifier.width(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "A",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = textSize.sp
                                )
                            }

                            // Slider to pick font size - call variable haptic on value changes
                            androidx.compose.material3.Slider(
                                value = textSize,
                                onValueChange = { new ->
                                    val prevStep = prevTextValue.toInt()
                                    val newStep = new.toInt()
                                    textSize = new
                                    if (newStep != prevStep) {
                                        prevTextValue = new
                                        val strength = (new - 8f) / (128f - 8f)
                                        val alpha = 0.35f
                                        smoothedTextStrength = smoothedTextStrength * (1f - alpha) + strength * alpha
                                        HapticUtil.performVariableTick(haptics, smoothedTextStrength)
                                    }
                                },
                                onValueChangeFinished = {
                                    HapticUtil.performClick(haptics)
                                },
                                valueRange = 8f..128f,
                                modifier = Modifier.width(240.dp)
                            )
                        } else {
                            // fallback for other tools - show nothing
                        }
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
                             HapticUtil.performToggleOn(haptics)
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
                                 // two-step click haptic on opening/closing secondary toolbar
                                 HapticUtil.performClick(haptics)
                             } else {
                                 currentTool = ToolType.PEN
                                 showPenOptions = false
                                 HapticUtil.performToggleOn(haptics)
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
                             // two-step click haptic for main toolbar expand/collapse
                             HapticUtil.performClick(haptics)
                         }
                     ) {
                         Icon(
                             painter = painterResource(
                                 id = if (expanded) R.drawable.icon else when (currentTool) {
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
                             HapticUtil.performToggleOn(haptics)
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

                     // Text tool: tapping while already selected toggles font-size options
                     IconButton(
                         modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                         onClick = {
                             if (currentTool == ToolType.TEXT) {
                                 showPenOptions = !showPenOptions
                                 // little click when opening the secondary toolbar
                                 HapticUtil.performClick(haptics)
                             } else {
                                 currentTool = ToolType.TEXT
                                 showPenOptions = false
                                 HapticUtil.performToggleOn(haptics)
                             }
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
    modifier: Modifier = Modifier,
    texts: List<com.sameerasw.canvas.data.TextItem> = emptyList(),
    penWidth: Float = 2.5f,
    textSize: Float = 16f,
    onAddStroke: ((DrawStroke) -> Unit)? = null,
    onRemoveStroke: ((predicate: (DrawStroke) -> Boolean) -> Unit)? = null,
    onAddText: ((com.sameerasw.canvas.data.TextItem) -> Unit)? = null,
    onUpdateText: ((com.sameerasw.canvas.data.TextItem) -> Unit)? = null,
    onRemoveText: ((Long) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    var drawingHapticJob by remember { mutableStateOf<Job?>(null) }

    // track movement speed on screen (px/s) to adapt haptic rate
    var lastMoveTime by remember { mutableStateOf(0L) }
    var lastMovePos by remember { mutableStateOf(Offset.Zero) }
    var currentSpeed by remember { mutableStateOf(0f) }

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
            .pointerInput(currentTool, penWidth, textSize) {
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
                                com.sameerasw.canvas.data.TextItem(
                                    id = selectedTextId!!,
                                    x = worldPos.x,
                                    y = worldPos.y,
                                    text = texts.first { it.id == selectedTextId!! }.text,
                                    size = texts.first { it.id == selectedTextId!! }.size
                                )
                            )
                            change.consume()
                            return@detectDragGestures
                        }

                        val worldPos = Offset((change.position.x - offsetX) / scale, (change.position.y - offsetY) / scale)
                        currentStroke.add(worldPos)
                        // update screen-space movement speed
                        val now = System.currentTimeMillis()
                        val pos = change.position
                        if (lastMoveTime == 0L) {
                            // Estimate speed from the movement since the previous pointer position on this event.
                            val prevPos = change.previousPosition
                            val dist = kotlin.math.hypot(pos.x - prevPos.x, pos.y - prevPos.y)
                            val assumedDt = 16L // assume ~16ms between pointer updates (60Hz) for initial estimate
                            val instSpeed = (dist / assumedDt) * 1000f
                            currentSpeed = instSpeed
                            lastMoveTime = now
                            lastMovePos = pos
                        } else {
                            val dt = (now - lastMoveTime).coerceAtLeast(1L)
                            val dist = kotlin.math.hypot(pos.x - lastMovePos.x, pos.y - lastMovePos.y)
                            val instSpeed = (dist / dt) * 1000f // px/s
                            // smooth speed value
                            currentSpeed = currentSpeed * 0.6f + instSpeed * 0.4f
                            lastMoveTime = now
                            lastMovePos = pos
                        }
                        // If drawing with pen and we don't yet have a haptic job, start one immediately
                        if (currentTool == ToolType.PEN && drawingHapticJob == null) {
                            drawingHapticJob = CoroutineScope(Dispatchers.Main).launch {
                                val minInterval = 10L    // fastest tick (ms)
                                val maxInterval = 450L   // slowest tick (ms)
                                val speedForMax = 300f  // px/s at which interval is minInterval
                                val stationaryThreshold = 60f // px/s below which we suppress ticks
                                while (isActive) {
                                    val sp = currentSpeed
                                    if (sp < stationaryThreshold) {
                                        // when nearly stationary, wait a bit before re-checking
                                        kotlinx.coroutines.delay(180L)
                                        continue
                                    }
                                    val t = ((maxInterval - minInterval) * (1f - (sp.coerceAtMost(speedForMax) / speedForMax))).toLong() + minInterval
                                    // immediate tick based on current speed
                                    HapticUtil.performLightTick(haptics)
                                    kotlinx.coroutines.delay(max(20L, t))
                                }
                            }
                        }
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
                                var removedAny = false
                                onRemoveStroke?.invoke { stroke ->
                                    val hit = stroke.points.any { point ->
                                        val distance = kotlin.math.hypot(
                                            worldPos.x - point.x,
                                            worldPos.y - point.y
                                        )
                                        distance < worldThreshold
                                    }
                                    if (hit) removedAny = true
                                    hit
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

                                    if (toRemove.isNotEmpty()) removedAny = true
                                    toRemove.forEach { id -> removeText(id) }
                                }

                                if (removedAny) {
                                    HapticUtil.performFadeOut(haptics)
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
                        // stop drawing haptics and reset speed
                        drawingHapticJob?.cancel()
                        drawingHapticJob = null
                        currentSpeed = 0f
                        lastMoveTime = 0L
                        lastMovePos = Offset.Zero
                        currentStroke.clear()
                        // finish moving
                        isMovingText = false
                        selectedTextId = null
                    },
                    onDragCancel = {
                        // stop drawing haptics and reset speed
                        drawingHapticJob?.cancel()
                        drawingHapticJob = null
                        currentSpeed = 0f
                        lastMoveTime = 0L
                        lastMovePos = Offset.Zero
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
                            // regular click haptic for dot add
                            HapticUtil.performClick(haptics)
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
                        // editing existing - preserve existing size
                        val existing = texts.firstOrNull { it.id == selectedTextId!! }
                        val sizeToUse = existing?.size ?: textSize
                        onUpdateText?.invoke(
                            com.sameerasw.canvas.data.TextItem(
                                id = selectedTextId!!,
                                x = pendingTextPosition.x,
                                y = pendingTextPosition.y,
                                text = pendingTextValue,
                                size = sizeToUse
                            )
                        )
                    } else {
                        // adding new - use the currently selected textSize
                        onAddText?.invoke(
                            com.sameerasw.canvas.data.TextItem(
                                x = pendingTextPosition.x,
                                y = pendingTextPosition.y,
                                text = pendingTextValue,
                                size = textSize
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
