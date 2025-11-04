package com.sameerasw.doodlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sameerasw.doodlist.ui.TaskViewModel
import com.sameerasw.doodlist.ui.theme.DoodListTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DoodListTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: TaskViewModel = viewModel()
                    val tasks by vm.tasks.collectAsState()

                    Column(modifier = Modifier.fillMaxSize().padding(8.dp).windowInsetsPadding(WindowInsets.systemBars)) {
                        val handwritingFont = FontFamily(Font(R.font.font))
                        Text("DoodList", style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = handwritingFont,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        ), modifier = Modifier.padding(8.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                        TaskList(tasks = tasks, vm = vm, modifier = Modifier.weight(1f))

                        // Add task area at bottom
                        AddTaskBar(vm = vm)
                    }
                }
            }
        }
    }
}

// Helper to serialize/deserialize strokes
fun serializePath(points: List<Offset>): String = points.joinToString(separator = ";") { "${it.x},${it.y}" }
fun deserializePath(data: String): List<Offset> = data.split(";").mapNotNull {
    val parts = it.split(",")
    if (parts.size != 2) return@mapNotNull null
    val x = parts[0].toFloatOrNull() ?: return@mapNotNull null
    val y = parts[1].toFloatOrNull() ?: return@mapNotNull null
    Offset(x, y)
}

@Composable
fun TaskList(tasks: List<com.sameerasw.doodlist.data.TaskEntity>, vm: TaskViewModel, modifier: Modifier = Modifier) {
    val sortedTasks = tasks.sortedBy { it.isDone }
    LazyColumn(modifier = modifier) {
        items(sortedTasks, key = { it.id }) { task ->
            TaskItem(task = task, vm = vm)
        }
    }
}

@Composable
fun TaskItem(task: com.sameerasw.doodlist.data.TaskEntity, vm: TaskViewModel) {
    var textHeightPx by remember { mutableStateOf(0) }
    var showEdit by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    // mutableStateListOf is not a State<T> and doesn't support property delegation with 'by'
    val restoredStrokes = remember { mutableStateListOf<List<Offset>>() }
    val scope = rememberCoroutineScope()

    // load strokes for this task
    LaunchedEffect(task.id) {
        vm.getStrokesFlow(task.id).collect { list ->
            restoredStrokes.clear()
            list.forEach { restoredStrokes.add(deserializePath(it.pathData)) }
        }
    }

    // Custom handwritten font
    val handwritingFont = FontFamily(Font(R.font.font))

    Box {
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 16.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showContextMenu = true
                    }
                )
            }) {
            Box(modifier = Modifier
                .height(72.dp)
                .fillMaxWidth()) {

                // Task text and drawing overlay
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = task.text,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 24.dp, end = 24.dp)
                            .onGloballyPositioned { coords ->
                                textHeightPx = coords.size.height
                            },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 24.sp,
                            color = if (task.isDone) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onBackground,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                            fontFamily = handwritingFont
                        )
                    )

                    DrawingOverlay(
                        modifier = Modifier.matchParentSize(),
                        isCompleted = task.isDone,
                        textHeight = textHeightPx.toFloat(),
                        restoredStrokes = restoredStrokes,
                        onStrokeComplete = { points ->
                            // persist stroke and mark done
                            scope.launch {
                                val data = serializePath(points)
                                vm.saveStroke(task.id, data)
                                vm.markDone(task.id)
                            }
                        }
                    )
                }
            }
        }

        // Context menu on long-press
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showEdit = true
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            )
            if (task.isDone) {
                DropdownMenuItem(
                    text = { Text("Mark undone") },
                    onClick = {
                        scope.launch {
                            vm.markUndone(task.id)
                        }
                        showContextMenu = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = "Mark undone")
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    vm.deleteTask(task.id)
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            )
        }
    }

    if (showEdit) {
        Dialog(onDismissRequest = { showEdit = false }) {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Edit task")
                    var editText by remember { mutableStateOf(task.text) }
                    TextField(value = editText, onValueChange = { editText = it })
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = {
                            vm.updateTask(task.copy(text = editText))
                            showEdit = false
                        }, colors = ButtonDefaults.buttonColors()) {
                            Text("Save")
                        }
                        Button(onClick = { showEdit = false }, modifier = Modifier.padding(start = 8.dp)) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawingOverlay(
    modifier: Modifier = Modifier,
    isCompleted: Boolean,
    textHeight: Float,
    restoredStrokes: List<List<Offset>> = emptyList(),
    onStrokeComplete: (List<Offset>) -> Unit
) {
    val points = remember { mutableStateListOf<Offset>() }
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    val strokeColor = MaterialTheme.colorScheme.onBackground
    var isValidDrawing by remember { mutableStateOf(true) }

    Canvas(modifier = modifier
        .onSizeChanged { canvasSize = it }
        .pointerInput(isCompleted, textHeight) {
            detectDragGestures(
                onDragStart = { offset: Offset ->
                    points.clear()
                    points.add(offset)
                    isValidDrawing = true
                },
                onDrag = { change: PointerInputChange, _: Offset ->
                    points.add(change.position)

                    // Check if movement is primarily vertical (vertical scroll)
                    if (points.size >= 2) {
                        val totalDx = abs(points.last().x - points.first().x)
                        val totalDy = abs(points.last().y - points.first().y)

                        // If vertical movement is significant compared to horizontal, it's a scroll
                        if (totalDy > totalDx * 0.5f) {
                            // This is a vertical scroll, don't consume and mark as invalid
                            isValidDrawing = false
                        } else {
                            // Horizontal movement, consume the event
                            change.consume()
                        }
                    }
                },
                onDragEnd = {
                    if (points.size >= 2 && isValidDrawing) {
                        val start = points.first()
                        val end = points.last()
                        val dx = end.x - start.x
                        val dy = end.y - start.y
                        val ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        val length = hypot(dx, dy)

                        val minX = points.minOf { it.x }
                        val maxX = points.maxOf { it.x }
                        val minY = points.minOf { it.y }
                        val maxY = points.maxOf { it.y }

                        val coverageRatio = if (canvasSize.width > 0) (maxX - minX) / canvasSize.width else 0f
                        val canvasCenterY = canvasSize.height / 2f
                        val intersectsText = (maxY >= (canvasCenterY - textHeight / 2f) && minY <= (canvasCenterY + textHeight / 2f))

                        // Only accept horizontal strokes (roughly left to right, angle near 0 degrees)
                        // Accept angles between -20 and 20 degrees (left to right)
                        // Also check for right to left (160-200 degrees) but we can be strict about left-to-right
                        val isHorizontal = abs(ang) < 25f
                        val isLeftToRight = dx > 0 // Ensure movement is left to right (positive dx)

                        // tuned heuristics: require at least 60% width coverage, horizontal direction, intersects text, and sufficient length
                        if (coverageRatio > 0.6f && isHorizontal && isLeftToRight && intersectsText && length > 40f) {
                            onStrokeComplete(points.toList())
                        }
                    }
                    points.clear()
                    isValidDrawing = true
                },
                onDragCancel = {
                    points.clear()
                    isValidDrawing = true
                }
            )
        }
    ) {
        // draw restored strokes with scribble and pressure effect
        restoredStrokes.forEach { stroke ->
            drawScribbleStroke(stroke, strokeColor)
        }

        // draw live stroke with scribble and pressure effect (only if valid drawing)
        if (points.isNotEmpty() && isValidDrawing) {
            drawScribbleStroke(points.toList(), strokeColor)
        }
    }
}

// Helper function to draw strokes with scribble effect and variable pressure
private fun DrawScope.drawScribbleStroke(stroke: List<Offset>, color: Color) {
    if (stroke.size < 2) return

    val random = Random(42) // Use seed for consistency

    // Create multiple overlapping paths with varying offsets for scribble effect
    val scribbleOffsets = listOf(-1.2f, -0.6f, 0f, 0.6f, 1.2f)
    val baseWidth = 2.5f

    scribbleOffsets.forEachIndexed { offsetIndex, scribbleOffset ->
        val path = Path().apply {
            moveTo(stroke.first().x + scribbleOffset, stroke.first().y + scribbleOffset)

            for (i in 1 until stroke.size) {
                val current = stroke[i]

                // Calculate stroke randomness for scribble effect - increased raggedness
                val randomOffsetX = (random.nextFloat() - 0.5f) * 1.5f * (6 - abs(offsetIndex - 2))
                val randomOffsetY = (random.nextFloat() - 0.5f) * 1.5f * (6 - abs(offsetIndex - 2))

                val scribbledX = current.x + scribbleOffset + randomOffsetX
                val scribbledY = current.y + scribbleOffset + randomOffsetY

                lineTo(scribbledX, scribbledY)
            }
        }

        // Calculate dynamic width based on offset (wider in middle, thinner at edges)
        val widthFactor = 1f - (abs(offsetIndex - 2) * 0.15f)
        val strokeWidth = baseWidth * widthFactor

        drawPath(path, color.copy(alpha = 0.65f + 0.35f * widthFactor), style = Stroke(width = strokeWidth))
    }

    // Add one final centered path with full opacity for definition
    val mainPath = Path().apply {
        moveTo(stroke.first().x, stroke.first().y)
        for (i in 1 until stroke.size) {
            lineTo(stroke[i].x, stroke[i].y)
        }
    }
    drawPath(mainPath, color, style = Stroke(width = 1.5f))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBar(vm: TaskViewModel) {
    var text by remember { mutableStateOf("") }
    val handwritingFont = FontFamily(Font(R.font.font))
    val underlineColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Text input without default styling
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("New task", style = MaterialTheme.typography.bodyLarge.copy(fontFamily = handwritingFont, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))) },
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = handwritingFont, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground),
                singleLine = true,
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    disabledContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.onBackground
                )
            )

            // Plus button - just text, no styling
            Text(
                "+",
                fontSize = 32.sp,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = handwritingFont,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clickable(enabled = text.isNotBlank()) {
                        vm.addTask(text.trim())
                        text = ""
                    }
            )
        }

        // Manual scribble underline
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .padding(top = 4.dp)) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.5f)
                // Create a wavy scribble-like line
                val steps = 20
                val waveHeight = size.height * 0.3f
                for (i in 0..steps) {
                    val x = (i.toFloat() / steps) * size.width
                    val y = size.height * 0.5f + waveHeight * kotlin.math.sin((i.toFloat() / steps) * Math.PI.toFloat() * 4f)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(path, underlineColor, style = Stroke(width = 2f))
        }
    }
}
