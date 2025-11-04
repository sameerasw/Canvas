package com.sameerasw.canvas.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.R
import com.sameerasw.canvas.model.ToolType

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToolbarFloating(
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    currentTool: ToolType,
    onHandTool: () -> Unit,
    onPenTool: () -> Unit,
    onEraserTool: () -> Unit,
    onTextTool: () -> Unit
) {
    HorizontalFloatingToolbar(
        modifier = Modifier,
        expanded = expanded,
        leadingContent = {
            // Hand tool
            IconButton(
                modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                onClick = onHandTool
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
                onClick = onPenTool
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
        content = {
            FilledIconButton(
                modifier = Modifier.width(if (expanded) 72.dp else 64.dp),
                onClick = onExpandToggle
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
        trailingContent = {
            // Eraser tool
            IconButton(
                modifier = Modifier.width(if (expanded) 64.dp else 48.dp),
                onClick = onEraserTool
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
                onClick = onTextTool
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

