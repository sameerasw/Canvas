package com.sameerasw.canvas.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Row

@Composable
fun TextInputDialog(
    visible: Boolean,
    title: String,
    buttonText: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(buttonText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },
            title = { Text(title) },
            text = {
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    label = { Text("Text") }
                )
            }
        )
    }
}

