package com.sameerasw.canvas

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sameerasw.canvas.util.CanvasSharingHelper
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.sameerasw.canvas.ui.theme.CanvasTheme
import com.sameerasw.canvas.ui.theme.CanvasThemeWithMode
import com.sameerasw.canvas.utils.HapticUtil

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ensure repository initialized
        SettingsRepository.init(this)
        enableEdgeToEdge()

        setContent {
            val themeMode = remember { mutableStateOf(SettingsRepository.getThemeMode()) }
            CanvasThemeWithMode(themeMode = themeMode.value) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(onThemeModeChange = { newMode ->
                        themeMode.value = newMode
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(onThemeModeChange: (SettingsRepository.ThemeMode) -> Unit = {}) {
    var level by remember { mutableStateOf(SettingsRepository.getHapticsLevel().value.toFloat()) }
    var pinTop by remember { mutableStateOf(SettingsRepository.getPinTopToolbar()) }
    var canvasBackground by remember { mutableStateOf(SettingsRepository.getCanvasBackground()) }
    var themeMode by remember { mutableStateOf(SettingsRepository.getThemeMode()) }
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        TopAppBar(
            title = { Text("Settings") },
            navigationIcon = {
                IconButton(
                    onClick = { (context as? Activity)?.finish() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_arrow_back_ios_new_24),
                        contentDescription = "Back",
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pin top toolbar toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pin top toolbar",
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(checked = pinTop, onCheckedChange = { new ->
                    pinTop = new
                    SettingsRepository.setPinTopToolbar(new)
                })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Haptics",
                    style = MaterialTheme.typography.titleMedium
                )

                // Show current selection at the far end
                val label = when (SettingsRepository.HapticsLevel.fromValue(level.toInt())) {
                    SettingsRepository.HapticsLevel.OFF -> "Off"
                    SettingsRepository.HapticsLevel.MIN -> "Min"
                    SettingsRepository.HapticsLevel.FULL -> "Full"
                }

                Text(text = label, modifier = Modifier.padding(start = 8.dp))
            }

            // Discrete slider with 3 steps 0..2
            Slider(
                value = level,
                onValueChange = { new ->
                    level = new
                    SettingsRepository.setHapticsLevel(SettingsRepository.HapticsLevel.fromValue(new.toInt()))
                },
                valueRange = 0f..2f,
                steps = 1 // for 3 values use steps = 1
            )

            // Canvas background setting
            Text(
                text = "Canvas background",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                val backgroundOptions = listOf(
                    "None",
                    "Dots",
                    "Lines"
                )
                backgroundOptions.forEachIndexed { index, label ->
                    ToggleButton(
                        checked = canvasBackground == SettingsRepository.CanvasBackgroundType.entries[index],
                        onCheckedChange = {
                            canvasBackground = SettingsRepository.CanvasBackgroundType.entries[index]
                            SettingsRepository.setCanvasBackground(canvasBackground)
                        },
                        modifier = Modifier.weight(1f),
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            backgroundOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Text(label)
                    }
                }
            }

            // Theme mode setting
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {
                val themeIcons = listOf(
                    R.drawable.rounded_light_mode_24,
                    R.drawable.rounded_cleaning_services_24,
                    R.drawable.rounded_mode_night_24
                )
                val themeOptions = SettingsRepository.ThemeMode.entries

                themeOptions.forEachIndexed { index, mode ->
                    ToggleButton(
                        checked = themeMode == mode,
                        onCheckedChange = {
                            themeMode = mode
                            SettingsRepository.setThemeMode(mode)
                            onThemeModeChange(mode)
                        },
                        modifier = Modifier.weight(1f),
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            themeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = themeIcons[index]),
                            contentDescription = mode.name
                        )
                    }
                }
            }

            // Progressive Blur setting
            var useBlur by remember { mutableStateOf(SettingsRepository.getUseBlur()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progressive blur",
                    style = MaterialTheme.typography.titleMedium
                )

                Switch(checked = useBlur, onCheckedChange = { new ->
                    useBlur = new
                    SettingsRepository.setUseBlur(new)
                })
            }

            // Share Canvas setting
            val canvasViewModel: CanvasViewModel = viewModel()
            val scope = rememberCoroutineScope()
            var showImportWarning by remember { mutableStateOf(false) }
            var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

            val exportLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
                onResult = { uri ->
                    uri?.let {
                        scope.launch {
                            val strokes = canvasViewModel.strokes.value
                            val texts = canvasViewModel.texts.value
                            val bgUri = canvasViewModel.backgroundImageUri.value
                            val model = CanvasModel(strokes, texts, bgUri)
                            CanvasSharingHelper.exportCanvas(context, model, it)
                            HapticUtil.performClick(haptics)
                            android.widget.Toast.makeText(context, "Canvas exported!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            val importLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument(),
                onResult = { uri ->
                    uri?.let {
                        pendingImportUri = it
                        showImportWarning = true
                    }
                }
            )

            if (showImportWarning) {
                AlertDialog(
                    onDismissRequest = { showImportWarning = false },
                    title = { Text(stringResource(R.string.import_warning_title)) },
                    text = { Text(stringResource(R.string.import_warning_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                pendingImportUri?.let { uri ->
                                    scope.launch {
                                        val model = CanvasSharingHelper.importCanvas(context, uri)
                                        if (model != null) {
                                            canvasViewModel.importCanvasState(model)
                                            android.widget.Toast.makeText(context, "Canvas imported!", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Failed to import canvas", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                showImportWarning = false
                            }
                        ) {
                            Text(stringResource(R.string.action_import))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportWarning = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            Text(
                text = stringResource(R.string.label_share_canvas),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        exportLauncher.launch("canvas-$timeStamp.canvas")
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_ios_share_24),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.action_export))
                }

                Button(
                    onClick = {
                        importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.rounded_download_24),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(stringResource(R.string.action_import))
                }
            }
        }
    }
}
