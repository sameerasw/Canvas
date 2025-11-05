package com.sameerasw.canvas.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sameerasw.canvas.model.DrawStroke
import com.sameerasw.canvas.model.ToolType
import com.sameerasw.canvas.data.TextItem
import com.sameerasw.canvas.ui.drawing.StrokeDrawer
import com.sameerasw.canvas.ui.drawing.StrokeDrawer.drawScribbleStroke
import com.sameerasw.canvas.ui.drawing.TextDrawer.drawStringWithFont
import com.sameerasw.canvas.ui.drawing.BackgroundDrawer
import com.sameerasw.canvas.utils.HapticUtil
import com.sameerasw.canvas.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.max

@Composable
fun DrawingCanvasScreen(
    currentTool: ToolType,
    strokes: List<DrawStroke>,
    texts: List<TextItem>,
    penWidth: Float,
    textSize: Float,
    currentColor: androidx.compose.ui.graphics.Color,
    currentPenStyle: com.sameerasw.canvas.model.PenStyle,
    currentShapeType: com.sameerasw.canvas.model.ShapeType,
    arrowWidth: Float = 5f,
    shapeWidth: Float = 5f,
    shapeFilled: Boolean = false,
    modifier: Modifier = Modifier,
    onAddStroke: ((DrawStroke) -> Unit)? = null,
    onRemoveStroke: ((predicate: (DrawStroke) -> Boolean) -> Unit)? = null,
    onRemoveText: ((Long) -> Unit)? = null,
    onShowTextDialog: ((Boolean, Offset) -> Unit)? = null,
    onShowTextOptions: ((Boolean, Long?) -> Unit)? = null,
    onUpdateCanvasTransform: ((scale: Float, offsetX: Float, offsetY: Float) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val currentStroke = remember { mutableStateListOf<Offset>() }
    val themeColor = currentColor.toArgb()
    val eraserRadius = 30f
    val shapeStartPoint = remember { mutableStateOf<Offset?>(null) }

    val scale = remember { mutableStateOf(1f) }
    val offsetX = remember { mutableStateOf(0f) }
    val offsetY = remember { mutableStateOf(0f) }
    val drawingHapticJob = remember { mutableStateOf<Job?>(null) }
    val lastMoveTime = remember { mutableStateOf(0L) }
    val lastMovePos = remember { mutableStateOf(Offset.Zero) }
    val currentSpeed = remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .pointerInput(currentTool) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // Only allow transform gestures when not actively drawing with other tools
                    if (currentTool == ToolType.HAND || zoom != 1f) {
                        val zoomSensitivity = 1.6f
                        val effectiveZoom = 1f + (zoom - 1f) * zoomSensitivity
                        val prevScale = scale.value
                        val newScale = (scale.value * effectiveZoom).coerceIn(0.25f, 6f)
                        val worldCx = (centroid.x - offsetX.value) / prevScale
                        val worldCy = (centroid.y - offsetY.value) / prevScale
                        scale.value = newScale
                        offsetX.value = centroid.x - worldCx * newScale + pan.x
                        offsetY.value = centroid.y - worldCy * newScale + pan.y
                        onUpdateCanvasTransform?.invoke(newScale, offsetX.value, offsetY.value)
                    }
                }
            }
            .pointerInput(currentTool, penWidth, textSize, currentColor, currentPenStyle, currentShapeType, arrowWidth, shapeWidth, shapeFilled) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentStroke.clear()
                        val worldStart = Offset((offset.x - offsetX.value) / scale.value, (offset.y - offsetY.value) / scale.value)
                        currentStroke.add(worldStart)
                    },
                    onDrag = { change: PointerInputChange, _ ->
                        val worldPos = Offset((change.position.x - offsetX.value) / scale.value, (change.position.y - offsetY.value) / scale.value)

                        if (currentTool == ToolType.HAND) {
                            offsetX.value += change.position.x - change.previousPosition.x
                            offsetY.value += change.position.y - change.previousPosition.y
                            onUpdateCanvasTransform?.invoke(scale.value, offsetX.value, offsetY.value)
                        } else {
                            currentStroke.add(worldPos)
                            val now = System.currentTimeMillis()
                            val pos = change.position
                            if (lastMoveTime.value == 0L) {
                                val prevPos = change.previousPosition
                                val dist = hypot(pos.x - prevPos.x, pos.y - prevPos.y)
                                val assumedDt = 16L
                                val instSpeed = (dist / assumedDt) * 1000f
                                currentSpeed.value = instSpeed
                                lastMoveTime.value = now
                                lastMovePos.value = pos
                            } else {
                                val dt = (now - lastMoveTime.value).coerceAtLeast(1L)
                                val dist = hypot(pos.x - lastMovePos.value.x, pos.y - lastMovePos.value.y)
                                val instSpeed = (dist / dt) * 1000f
                                currentSpeed.value = currentSpeed.value * 0.6f + instSpeed * 0.4f
                                lastMoveTime.value = now
                                lastMovePos.value = pos
                            }

                            if (currentTool == ToolType.PEN && drawingHapticJob.value == null) {
                                drawingHapticJob.value = CoroutineScope(Dispatchers.Main).launch {
                                    val minInterval = 10L
                                    val maxInterval = 450L
                                    val speedForMax = 300f
                                    val stationaryThreshold = 60f
                                    while (isActive) {
                                        val sp = currentSpeed.value
                                        if (sp < stationaryThreshold) {
                                            delay(180L)
                                            continue
                                        }
                                        val t = ((maxInterval - minInterval) * (1f - (sp.coerceAtMost(speedForMax) / speedForMax))).toLong() + minInterval
                                        HapticUtil.performLightTick(haptics)
                                        delay(max(20L, t))
                                    }
                                }
                            }

                            when (currentTool) {
                                ToolType.ERASER -> {
                                    val worldThreshold = eraserRadius / scale.value
                                    var removedAny = false
                                    onRemoveStroke?.invoke { stroke ->
                                        val hit = when {
                                            // For shapes and arrows, check bounding box and geometry
                                            stroke.isArrow || stroke.shapeType != null -> {
                                                if (stroke.points.size >= 2) {
                                                    val start = stroke.points.first()
                                                    val end = stroke.points.last()
                                                    // Check if eraser touches the line/shape
                                                    val minX = kotlin.math.min(start.x, end.x) - worldThreshold
                                                    val maxX = kotlin.math.max(start.x, end.x) + worldThreshold
                                                    val minY = kotlin.math.min(start.y, end.y) - worldThreshold
                                                    val maxY = kotlin.math.max(start.y, end.y) + worldThreshold
                                                    worldPos.x in minX..maxX && worldPos.y in minY..maxY
                                                } else false
                                            }
                                            // For regular strokes, check all line segments
                                            else -> {
                                                stroke.points.any { point ->
                                                    val distance = hypot(worldPos.x - point.x, worldPos.y - point.y)
                                                    distance < worldThreshold
                                                } || stroke.points.zipWithNext().any { (p1, p2) ->
                                                    // Check distance to line segment
                                                    val dx = p2.x - p1.x
                                                    val dy = p2.y - p1.y
                                                    val lengthSquared = dx * dx + dy * dy
                                                    if (lengthSquared == 0f) {
                                                        hypot(worldPos.x - p1.x, worldPos.y - p1.y) < worldThreshold
                                                    } else {
                                                        val t = ((worldPos.x - p1.x) * dx + (worldPos.y - p1.y) * dy) / lengthSquared
                                                        val clampedT = t.coerceIn(0f, 1f)
                                                        val closestX = p1.x + clampedT * dx
                                                        val closestY = p1.y + clampedT * dy
                                                        hypot(worldPos.x - closestX, worldPos.y - closestY) < worldThreshold
                                                    }
                                                }
                                            }
                                        }
                                        if (hit) removedAny = true
                                        hit
                                    }

                                    onRemoveText?.let { removeText ->
                                        val toRemove = texts.filter { text ->
                                            val dx = worldPos.x - text.x
                                            val dy = worldPos.y - text.y
                                            val distance = hypot(dx, dy)
                                            distance < worldThreshold + (text.size * 0.5f)
                                        }.map { it.id }

                                        if (toRemove.isNotEmpty()) removedAny = true
                                        toRemove.forEach { id -> removeText(id) }
                                    }

                                    if (removedAny) {
                                        HapticUtil.performFadeOut(haptics)
                                    }
                                }
                                else -> {}
                            }
                        }
                        change.consume()
                    },
                    onDragEnd = {
                        when (currentTool) {
                            ToolType.PEN -> {
                                if (currentStroke.size >= 2) {
                                    val newStroke = DrawStroke(
                                        currentStroke.toList(),
                                        currentColor,
                                        width = penWidth,
                                        style = currentPenStyle
                                    )
                                    onAddStroke?.invoke(newStroke)
                                }
                            }
                            ToolType.ARROW -> {
                                if (currentStroke.size >= 2) {
                                    val start = currentStroke.first()
                                    val end = currentStroke.last()
                                    val newStroke = DrawStroke(
                                        listOf(start, end),
                                        currentColor,
                                        width = arrowWidth,
                                        isArrow = true
                                    )
                                    onAddStroke?.invoke(newStroke)
                                }
                            }
                            ToolType.SHAPE -> {
                                if (currentStroke.size >= 2) {
                                    val start = currentStroke.first()
                                    val end = currentStroke.last()
                                    val newStroke = DrawStroke(
                                        listOf(start, end),
                                        currentColor,
                                        width = shapeWidth,
                                        shapeType = currentShapeType,
                                        isFilled = shapeFilled
                                    )
                                    onAddStroke?.invoke(newStroke)
                                }
                            }
                            else -> {}
                        }
                        drawingHapticJob.value?.cancel()
                        drawingHapticJob.value = null
                        currentSpeed.value = 0f
                        lastMoveTime.value = 0L
                        lastMovePos.value = Offset.Zero
                        currentStroke.clear()
                        shapeStartPoint.value = null
                    },
                    onDragCancel = {
                        drawingHapticJob.value?.cancel()
                        drawingHapticJob.value = null
                        currentSpeed.value = 0f
                        lastMoveTime.value = 0L
                        lastMovePos.value = Offset.Zero
                        currentStroke.clear()
                        shapeStartPoint.value = null
                    }
                )
            }
            .pointerInput(currentTool) {
                detectTapGestures(
                    onLongPress = { offset ->
                        if (currentTool == ToolType.TEXT) {
                            val world = Offset((offset.x - offsetX.value) / scale.value, (offset.y - offsetY.value) / scale.value)
                            val hit = texts.find { text ->
                                val half = text.size
                                world.x >= text.x - half && world.x <= text.x + half &&
                                        world.y >= text.y - half && world.y <= text.y + half
                            }

                            if (hit != null) {
                                onShowTextOptions?.invoke(true, hit.id)
                            } else {
                                onShowTextDialog?.invoke(true, world)
                            }
                        }
                    },
                    onTap = { offset ->
                        val world = Offset((offset.x - offsetX.value) / scale.value, (offset.y - offsetY.value) / scale.value)

                        when (currentTool) {
                            ToolType.PEN -> {
                                val delta = 0.5f
                                val p1 = world
                                val p2 = Offset(world.x + delta, world.y + delta)
                                onAddStroke?.invoke(DrawStroke(listOf(p1, p2), currentColor, width = penWidth, style = currentPenStyle))
                                HapticUtil.performClick(haptics)
                                return@detectTapGestures
                            }
                            ToolType.TEXT -> {
                                // Check if tapping on existing text
                                val hit = texts.find { text ->
                                    val half = text.size
                                    world.x >= text.x - half && world.x <= text.x + half &&
                                            world.y >= text.y - half && world.y <= text.y + half
                                }
                                
                                if (hit != null) {
                                    // Edit existing text
                                    onShowTextOptions?.invoke(true, hit.id)
                                } else {
                                    // Add new text
                                    onShowTextDialog?.invoke(true, world)
                                }
                                HapticUtil.performClick(haptics)
                                return@detectTapGestures
                            }
                            else -> {}
                        }
                    }
                )
            }
    ) {
        // Draw background pattern with canvas transform
        val backgroundType = SettingsRepository.getCanvasBackground()
        BackgroundDrawer.drawBackgroundOnCompose(this, backgroundType, size.width, size.height, scale.value, offsetX.value, offsetY.value)

        strokes.forEach { stroke ->
            val screenPoints = stroke.points.map { world -> Offset(world.x * scale.value + offsetX.value, world.y * scale.value + offsetY.value) }
            when {
                stroke.isArrow && screenPoints.size >= 2 -> {
                    with(StrokeDrawer) {
                        drawArrow(screenPoints.first(), screenPoints.last(), stroke.color, stroke.width * scale.value)
                    }
                }
                stroke.shapeType != null && screenPoints.size >= 2 -> {
                    with(StrokeDrawer) {
                        drawShape(screenPoints.first(), screenPoints.last(), stroke.shapeType, stroke.color, stroke.width * scale.value, stroke.isFilled)
                    }
                }
                screenPoints.size >= 2 -> {
                    drawScribbleStroke(screenPoints, stroke.color, stroke.width * scale.value, stroke.style)
                }
            }
        }

        texts.forEach { t ->
            val sx = t.x * scale.value + offsetX.value
            val sy = t.y * scale.value + offsetY.value
            val fontSize = t.size * scale.value
            drawStringWithFont(context, t.text, sx, sy, fontSize, themeColor)
        }

        when (currentTool) {
            ToolType.PEN -> {
                if (currentStroke.size >= 2) {
                    val screenPoints = currentStroke.map { world -> Offset(world.x * scale.value + offsetX.value, world.y * scale.value + offsetY.value) }
                    drawScribbleStroke(screenPoints, currentColor, penWidth * scale.value, currentPenStyle)
                }
            }
            ToolType.ARROW -> {
                if (currentStroke.size >= 2) {
                    val start = Offset(currentStroke.first().x * scale.value + offsetX.value, currentStroke.first().y * scale.value + offsetY.value)
                    val end = Offset(currentStroke.last().x * scale.value + offsetX.value, currentStroke.last().y * scale.value + offsetY.value)
                    with(StrokeDrawer) {
                        drawArrow(start, end, currentColor, arrowWidth * scale.value)
                    }
                }
            }
            ToolType.SHAPE -> {
                if (currentStroke.size >= 2) {
                    val start = Offset(currentStroke.first().x * scale.value + offsetX.value, currentStroke.first().y * scale.value + offsetY.value)
                    val end = Offset(currentStroke.last().x * scale.value + offsetX.value, currentStroke.last().y * scale.value + offsetY.value)
                    with(StrokeDrawer) {
                        drawShape(start, end, currentShapeType, currentColor, shapeWidth * scale.value, shapeFilled)
                    }
                }
            }
            ToolType.ERASER -> {
                if (currentStroke.isNotEmpty()) {
                    val last = currentStroke.last()
                    val center = Offset(last.x * scale.value + offsetX.value, last.y * scale.value + offsetY.value)
                    drawCircle(color = currentColor.copy(alpha = 0.3f), radius = eraserRadius, center = center)
                }
            }
            else -> {}
        }
    }
}

