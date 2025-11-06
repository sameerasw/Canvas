package com.sameerasw.canvas

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.sameerasw.canvas.model.ToolType
import com.sameerasw.canvas.ui.components.dialogs.AboutDialog
import com.sameerasw.canvas.ui.components.panels.PenWidthOptionsPanel
import com.sameerasw.canvas.ui.components.panels.TextSizeOptionsPanel
import com.sameerasw.canvas.ui.components.dialogs.TextInputDialog
import com.sameerasw.canvas.ui.components.dialogs.TextOptionsDialog
import com.sameerasw.canvas.ui.components.bottomtoolbar.ToolbarFloating
import com.sameerasw.canvas.ui.components.toptoolbar.TopMenuButtons
import com.sameerasw.canvas.ui.components.toptoolbar.TopOverlayToolbar
import com.sameerasw.canvas.ui.drawing.BitmapExportHelper
import com.sameerasw.canvas.ui.drawing.BitmapStorageHelper
import com.sameerasw.canvas.ui.screens.DrawingCanvasScreen
import com.sameerasw.canvas.ui.theme.CanvasTheme
import com.sameerasw.canvas.utils.HapticUtil
import com.sameerasw.canvas.data.TextItem
import com.sameerasw.canvas.ui.components.panels.ArrowOptionsPanel
import com.sameerasw.canvas.ui.components.panels.ShapeOptionsPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: CanvasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init settings repository early so HapticUtil can read preferences
        SettingsRepository.init(this)
        enableEdgeToEdge()
        setContent {
            val themeMode = remember { mutableStateOf(SettingsRepository.getThemeMode()) }

            // Monitor theme changes from settings
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(500)
                    val currentTheme = SettingsRepository.getThemeMode()
                    if (currentTheme != themeMode.value) {
                        themeMode.value = currentTheme
                    }
                }
            }

            com.sameerasw.canvas.ui.theme.CanvasThemeWithMode(themeMode = themeMode.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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

@Composable
fun CanvasApp(viewModel: CanvasViewModel) {
    var currentTool by remember { mutableStateOf(ToolType.PEN) }
    val strokes by viewModel.strokes.collectAsState()
    val texts by viewModel.texts.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var topMenuOpen by remember { mutableStateOf(false) }
    var pinTopToolbar by remember { mutableStateOf(SettingsRepository.getPinTopToolbar()) }
    var penWidth by remember { mutableStateOf(2.5f) }
    var prevPenValue by remember { mutableStateOf(penWidth) }
    var smoothedStrength by remember { mutableStateOf((penWidth - 1f) / (48f - 1f)) }
    var textSize by remember { mutableStateOf(16f) }
    var prevTextValue by remember { mutableStateOf(textSize) }
    var smoothedTextStrength by remember { mutableStateOf((textSize - 8f) / (128f - 8f)) }
    var showPenOptions by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showTextOptions by remember { mutableStateOf(false) }
    var pendingTextPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingTextValue by remember { mutableStateOf("") }
    var selectedTextId by remember { mutableStateOf<Long?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    
    var currentColor by remember { mutableStateOf(androidx.compose.ui.graphics.Color.Black) }
    var currentPenStyle by remember { mutableStateOf(com.sameerasw.canvas.model.PenStyle.NORMAL) }
    var currentShapeType by remember { mutableStateOf(com.sameerasw.canvas.model.ShapeType.RECTANGLE) }
    var shapeFilled by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    // Tool-specific widths
    var arrowWidth by remember { mutableStateOf(5f) }
    var shapeWidth by remember { mutableStateOf(5f) }

    // Canvas viewport tracking (pixels)
    var canvasScale by remember { mutableStateOf(1f) }
    var canvasOffsetX by remember { mutableStateOf(0f) }
    var canvasOffsetY by remember { mutableStateOf(0f) }
    var canvasViewSize by remember { mutableStateOf(IntSize(1, 1)) }

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        DrawingCanvasScreen(
            currentTool = currentTool,
            strokes = strokes,
            texts = texts,
            penWidth = penWidth,
            textSize = textSize,
            currentColor = currentColor,
            currentPenStyle = currentPenStyle,
            currentShapeType = currentShapeType,
            arrowWidth = arrowWidth,
            shapeWidth = shapeWidth,
            shapeFilled = shapeFilled,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    canvasViewSize = size
                },
            onAddStroke = { viewModel.addStroke(it) },
            onRemoveStroke = { predicate -> viewModel.removeStroke(predicate) },
            onRemoveText = { viewModel.removeText(it) },
            onShowTextDialog = { show, pos ->
                if (show) {
                    pendingTextPosition = pos
                    pendingTextValue = ""
                    selectedTextId = null
                }
                showTextDialog = show
            },
            onShowTextOptions = { show, id ->
                if (show) selectedTextId = id
                showTextOptions = show
            },
            onUpdateCanvasTransform = { scale, offX, offY ->
                canvasScale = scale
                canvasOffsetX = offX
                canvasOffsetY = offY
            }
        )

        // Top overlay toolbar
        // If pinned, always visible; otherwise visible when expanded
        val topVisible = pinTopToolbar || expanded
        val canRedoState by viewModel.canRedo.collectAsState()
        val canUndoState by viewModel.canUndo.collectAsState()
        TopOverlayToolbar(
            visible = topVisible,
            menuOpen = topMenuOpen,
            onMenuToggle = { topMenuOpen = !topMenuOpen },
            onUndo = {
                // use viewModel.undo() so redo stack is populated and redo becomes available
                viewModel.undo()
            },
            // new redo wiring
            canUndo = canUndoState,
            canRedo = canRedoState,
            onRedo = { viewModel.redo() },
            menuContent = {
                TopMenuButtons(
                    visible = topMenuOpen,
                    onShare = {
                        topMenuOpen = false
                        CoroutineScope(Dispatchers.Main).launch {
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val filename = "canvas_full_$ts.png"
                            val bmp = BitmapExportHelper.createBitmapFromData(context, strokes, texts, outputWidth = canvasViewSize.width.coerceAtLeast(1), outputHeight = canvasViewSize.height.coerceAtLeast(1))
                            if (bmp != null) {
                                val uri = BitmapStorageHelper.saveBitmapToCacheAndGetUri(context, bmp, filename, Bitmap.CompressFormat.PNG)
                                if (uri != null) {
                                    // Serialize strokes and texts to JSON for passing to CropActivity
                                    val gson = com.sameerasw.canvas.util.GsonProvider.create()
                                    val strokesJson = gson.toJson(strokes)
                                    val textsJson = gson.toJson(texts)

                                    val intent = Intent(context, CropActivity::class.java).apply {
                                        putExtra("image_uri", uri.toString())
                                        putExtra("is_share", true)
                                        putExtra("strokes_json", strokesJson)
                                        putExtra("texts_json", textsJson)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Failed to export image", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Nothing to export", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onClear = {
                        topMenuOpen = false
                        showClearConfirm = true
                    },
                    onSettings = {
                        topMenuOpen = false
                        // open settings activity
                        val intent = Intent(context, SettingsActivity::class.java)
                        context.startActivity(intent)
                    },
                    onAbout = {
                        topMenuOpen = false
                        showAboutDialog = true
                    }
                )
            }
        )

        // Confirmation dialog for clearing canvas
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("Reset canvas?") },
                text = { Text("This will clear all strokes and text from the canvas. This action cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearAll()
                        // persist the cleared state
                        viewModel.save()
                        showClearConfirm = false
                        Toast.makeText(context, "Canvas cleared", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // About dialog
        if (showAboutDialog) {
            AboutDialog(
                onDismissRequest = { showAboutDialog = false },
                onToggleDeveloperMode = {
                    Toast.makeText(context, "Developer mode toggled", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            com.sameerasw.canvas.ui.components.ColorPicker(
                visible = showColorPicker,
                selectedColor = currentColor,
                onColorSelected = { currentColor = it }
            )

            PenWidthOptionsPanel(
                visible = showPenOptions && currentTool == ToolType.PEN,
                selectedStyle = currentPenStyle,
                penWidth = penWidth,
                onStyleSelected = { currentPenStyle = it },
                onPenWidthChange = { new ->
                    val prevStep = prevPenValue.toInt()
                    val newStep = new.toInt()
                    penWidth = new
                    if (newStep != prevStep) {
                        prevPenValue = new
                        val strength = (new - 1f) / (48f - 1f)
                        val alpha = 0.35f
                        smoothedStrength =
                            smoothedStrength * (1f - alpha) + strength * alpha
                        HapticUtil.performVariableTick(haptics, smoothedStrength)
                    }
                }
            )

            ArrowOptionsPanel(
                visible = showPenOptions && currentTool == ToolType.ARROW,
                arrowWidth = arrowWidth,
                onArrowWidthChange = { arrowWidth = it }
            )

            ShapeOptionsPanel(
                visible = showPenOptions && currentTool == ToolType.SHAPE,
                selectedShape = currentShapeType,
                shapeWidth = shapeWidth,
                shapeFilled = shapeFilled,
                onShapeSelected = { currentShapeType = it },
                onShapeWidthChange = { shapeWidth = it },
                onShapeFilledChange = { shapeFilled = it }
            )

            TextSizeOptionsPanel(
                visible = showPenOptions && currentTool == ToolType.TEXT,
                textSize = textSize,
                onTextSizeChange = { new ->
                    val prevStep = prevTextValue.toInt()
                    val newStep = new.toInt()
                    textSize = new
                    if (newStep != prevStep) {
                        prevTextValue = new
                        val strength = (new - 8f) / (128f - 8f)
                        val alpha = 0.35f
                        smoothedTextStrength =
                            smoothedTextStrength * (1f - alpha) + strength * alpha
                        HapticUtil.performVariableTick(haptics, smoothedTextStrength)
                    }
                }
            )

            Spacer(modifier = Modifier.size(6.dp))

            ToolbarFloating(
                expanded = expanded,
                onExpandToggle = {
                    val new = !expanded
                    pinTopToolbar = SettingsRepository.getPinTopToolbar()
                    expanded = new
                    if (!new) {
                        showPenOptions = false
                        showColorPicker = false
                    }
                    HapticUtil.performClick(haptics)
                },
                currentTool = currentTool,
                onHandTool = {
                    currentTool = ToolType.HAND
                    showPenOptions = false
                    showColorPicker = false
                    HapticUtil.performToggleOn(haptics)
                },
                onPenTool = {
                    if (currentTool == ToolType.PEN) {
                        showPenOptions = !showPenOptions
                        showColorPicker = !showColorPicker
                        HapticUtil.performClick(haptics)
                    } else {
                        currentTool = ToolType.PEN
                        showPenOptions = false
                        showColorPicker = false
                        HapticUtil.performToggleOn(haptics)
                    }
                },
                onEraserTool = {
                    currentTool = ToolType.ERASER
                    showPenOptions = false
                    showColorPicker = false
                    HapticUtil.performToggleOn(haptics)
                },
                onTextTool = {
                    if (currentTool == ToolType.TEXT) {
                        showPenOptions = !showPenOptions
                        showColorPicker = showPenOptions
                        HapticUtil.performClick(haptics)
                    } else {
                        currentTool = ToolType.TEXT
                        showPenOptions = true
                        showColorPicker = true
                        HapticUtil.performToggleOn(haptics)
                    }
                },
                onArrowTool = {
                    if (currentTool == ToolType.ARROW) {
                        showColorPicker = !showColorPicker
                        showPenOptions = !showPenOptions
                        HapticUtil.performClick(haptics)
                    } else {
                        currentTool = ToolType.ARROW
                        showPenOptions = true
                        showColorPicker = true
                        HapticUtil.performToggleOn(haptics)
                    }
                },
                onShapeTool = {
                    if (currentTool == ToolType.SHAPE) {
                        showColorPicker = !showColorPicker
                        showPenOptions = !showPenOptions
                        HapticUtil.performClick(haptics)
                    } else {
                        currentTool = ToolType.SHAPE
                        showPenOptions = true
                        showColorPicker = true
                        HapticUtil.performToggleOn(haptics)
                    }
                }
            )
        }
    }

    // Text dialogs
    TextOptionsDialog(
        visible = showTextOptions,
        onEdit = {
            val hit = texts.firstOrNull { it.id == selectedTextId }
            if (hit != null) {
                pendingTextValue = hit.text
                pendingTextPosition = Offset(hit.x, hit.y)
                showTextDialog = true
            }
            showTextOptions = false
        },
        onMove = {
            showTextOptions = false
        },
        onDelete = {
            if (selectedTextId != null) {
                viewModel.removeText(selectedTextId!!)
                selectedTextId = null
            }
            showTextOptions = false
        },
        onCancel = {
            selectedTextId = null
            showTextOptions = false
        }
    )

    TextInputDialog(
        visible = showTextDialog,
        title = if (selectedTextId != null) "Edit Text" else "Add Text",
        buttonText = if (selectedTextId != null) "Save" else "Add",
        currentValue = pendingTextValue,
        onValueChange = { pendingTextValue = it },
        onConfirm = {
            if (selectedTextId != null) {
                val existing = texts.firstOrNull { it.id == selectedTextId!! }
                val sizeToUse = existing?.size ?: textSize
                val colorToUse = existing?.color ?: currentColor
                viewModel.updateText(
                    TextItem(
                        id = selectedTextId!!,
                        x = pendingTextPosition.x,
                        y = pendingTextPosition.y,
                        text = pendingTextValue,
                        size = sizeToUse,
                        color = colorToUse
                    )
                )
            } else {
                viewModel.addText(
                    TextItem(
                        x = pendingTextPosition.x,
                        y = pendingTextPosition.y,
                        text = pendingTextValue,
                        size = textSize,
                        color = currentColor
                    )
                )
            }
            showTextDialog = false
            selectedTextId = null
        },
        onDismiss = {
            showTextDialog = false
            selectedTextId = null
        }
    )
}
