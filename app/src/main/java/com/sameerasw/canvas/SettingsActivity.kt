package com.sameerasw.canvas

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.sameerasw.canvas.ui.theme.CanvasTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ensure repository initialized
        SettingsRepository.init(this)

        setContent {
            CanvasTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen() {
    var level by remember { mutableStateOf(SettingsRepository.getHapticsLevel().value.toFloat()) }
    var pinTop by remember { mutableStateOf(SettingsRepository.getPinTopToolbar()) }
    var canvasBackground by remember { mutableStateOf(SettingsRepository.getCanvasBackground()) }
    val context = LocalContext.current

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
        }
    }
}
