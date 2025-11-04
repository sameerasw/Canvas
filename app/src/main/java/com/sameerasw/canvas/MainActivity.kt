package com.sameerasw.canvas

import android.content.Intent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.model.ToolType
import com.sameerasw.canvas.ui.components.AboutDialog
import com.sameerasw.canvas.ui.components.PenWidthOptionsPanel
import com.sameerasw.canvas.ui.components.TextSizeOptionsPanel
import com.sameerasw.canvas.ui.components.TextInputDialog
import com.sameerasw.canvas.ui.components.TextOptionsDialog
import com.sameerasw.canvas.ui.components.ToolbarFloating
import com.sameerasw.canvas.ui.components.TopMenuButtons
import com.sameerasw.canvas.ui.components.TopOverlayToolbar
import com.sameerasw.canvas.ui.drawing.BitmapExportHelper
import com.sameerasw.canvas.ui.drawing.BitmapStorageHelper
import com.sameerasw.canvas.ui.screens.DrawingCanvasScreen
import com.sameerasw.canvas.ui.theme.CanvasTheme
import com.sameerasw.canvas.utils.HapticUtil
import com.sameerasw.canvas.data.TextItem
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
        enableEdgeToEdge()
        setContent {
            CanvasTheme {
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
    // show confirmation when user taps Clear in top menu
    var showClearConfirm by remember { mutableStateOf(false) }
    // show About dialog
    var showAboutDialog by remember { mutableStateOf(false) }

    // Canvas viewport tracking (pixels)
    var canvasScale by remember { mutableStateOf(1f) }
    var canvasOffsetX by remember { mutableStateOf(0f) }
    var canvasOffsetY by remember { mutableStateOf(0f) }
    var canvasViewSize by remember { mutableStateOf(IntSize(1, 1)) }

    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        // Canvas area - bottom layer
        DrawingCanvasScreen(
            currentTool = currentTool,
            strokes = strokes,
            texts = texts,
            penWidth = penWidth,
            textSize = textSize,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    canvasViewSize = size
                },
            onAddStroke = { viewModel.addStroke(it) },
            onRemoveStroke = { predicate -> viewModel.removeStroke(predicate) },
            onAddText = { viewModel.addText(it) },
            onUpdateText = { viewModel.updateText(it) },
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
        TopOverlayToolbar(
            visible = expanded,
            menuOpen = topMenuOpen,
            onMenuToggle = { topMenuOpen = !topMenuOpen },
            onUndo = {
                if (strokes.isNotEmpty()) {
                    viewModel.setStrokes(strokes.dropLast(1))
                }
            },
            menuContent = {
                TopMenuButtons(
                    visible = topMenuOpen,
                    onShare = {
                        topMenuOpen = false
                        CoroutineScope(Dispatchers.Main).launch {
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val filename = "doodlist_full_$ts.png"
                            val bmp = BitmapExportHelper.createBitmapFromData(context, strokes, texts, outputWidth = canvasViewSize.width.coerceAtLeast(1), outputHeight = canvasViewSize.height.coerceAtLeast(1))
                            if (bmp != null) {
                                val uri = BitmapStorageHelper.saveBitmapToCacheAndGetUri(context, bmp, filename, android.graphics.Bitmap.CompressFormat.PNG)
                                if (uri != null) {
                                    val intent = Intent(context, CropActivity::class.java).apply {
                                        putExtra("image_uri", uri.toString())
                                        putExtra("is_share", true)
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
                    onSave = {
                        topMenuOpen = false
                        CoroutineScope(Dispatchers.Main).launch {
                            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val filename = "doodlist_full_$ts.png"
                            val bmp = BitmapExportHelper.createBitmapFromData(context, strokes, texts, outputWidth = canvasViewSize.width.coerceAtLeast(1), outputHeight = canvasViewSize.height.coerceAtLeast(1))
                            if (bmp != null) {
                                val uri = BitmapStorageHelper.saveBitmapToCacheAndGetUri(context, bmp, filename, android.graphics.Bitmap.CompressFormat.PNG)
                                if (uri != null) {
                                    val intent = Intent(context, CropActivity::class.java).apply {
                                        putExtra("image_uri", uri.toString())
                                        putExtra("is_share", false)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Failed to export image", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Nothing to save", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onClear = {
                        topMenuOpen = false
                        showClearConfirm = true
                    },
                    onSettings = {
                        topMenuOpen = false
                        Toast.makeText(context, "Settings", Toast.LENGTH_SHORT).show()
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

        // Bottom toolbar column
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pen width options
            PenWidthOptionsPanel(
                visible = showPenOptions && currentTool == ToolType.PEN,
                penWidth = penWidth,
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

            // Text size options
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

            // Main floating toolbar
            ToolbarFloating(
                expanded = expanded,
                onExpandToggle = {
                    val new = !expanded
                    expanded = new
                    if (!new) showPenOptions = false
                    HapticUtil.performClick(haptics)
                },
                currentTool = currentTool,
                onHandTool = {
                    currentTool = ToolType.HAND
                    showPenOptions = false
                    HapticUtil.performToggleOn(haptics)
                },
                onPenTool = {
                    if (currentTool == ToolType.PEN) {
                        showPenOptions = !showPenOptions
                        HapticUtil.performClick(haptics)
                    } else {
                        currentTool = ToolType.PEN
                        showPenOptions = false
                        HapticUtil.performToggleOn(haptics)
                    }
                },
                onEraserTool = {
                    currentTool = ToolType.ERASER
                    showPenOptions = false
                    HapticUtil.performToggleOn(haptics)
                },
                onTextTool = {
                    if (currentTool == ToolType.TEXT) {
                        showPenOptions = !showPenOptions
                        HapticUtil.performClick(haptics)
                    } else {
                        currentTool = ToolType.TEXT
                        showPenOptions = false
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
                viewModel.updateText(
                    TextItem(
                        id = selectedTextId!!,
                        x = pendingTextPosition.x,
                        y = pendingTextPosition.y,
                        text = pendingTextValue,
                        size = sizeToUse
                    )
                )
            } else {
                viewModel.addText(
                    TextItem(
                        x = pendingTextPosition.x,
                        y = pendingTextPosition.y,
                        text = pendingTextValue,
                        size = textSize
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
