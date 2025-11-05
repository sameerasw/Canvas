package com.sameerasw.canvas.ui.components.dialogs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row

@Composable
fun TextOptionsDialog(
    visible: Boolean,
    onEdit: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text("Text options") },
            text = { Text("Choose an action for the selected text") },
            confirmButton = {
                TextButton(onClick = onEdit) { Text("Edit") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onMove) { Text("Move") }
                    TextButton(onClick = onDelete) { Text("Delete") }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
            }
        )
    }
}

