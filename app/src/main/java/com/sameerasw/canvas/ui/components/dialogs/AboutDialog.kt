package com.sameerasw.canvas.ui.components.dialogs

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.canvas.R
import com.sameerasw.canvas.ui.components.ContributorsCarousel

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit,
    onToggleDeveloperMode: () -> Unit = {},
    appName: String = "Canvas",
    developerName: String = "Sameera Wijerathna",
    description: String = "It's a canvas for your imagination.",
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    val versionName = try {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
    } catch (_: Exception) {
        "Unknown"
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                "$appName v$versionName",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Left
                )

                Spacer(modifier = Modifier.height(24.dp))

                Image(
                    painter = painterResource(id = R.drawable.avatar),
                    contentDescription = "Developer Avatar",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    // Trigger haptic feedback
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                    // Show toast
                                    Toast.makeText(context, "Developer Mode", Toast.LENGTH_SHORT).show()

                                    // Toggle developer mode visibility
                                    onToggleDeveloperMode()
                                }
                            )
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Developed by $developerName",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    "With ❤\uFE0F from \uD83C\uDDF1\uD83C\uDDF0",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                ContributorsCarousel()

            }
        },
        dismissButton = {
            OutlinedButton(onClick = {
                val websiteUrl = "https://www.sameerasw.com"
                val intent = Intent(Intent.ACTION_VIEW, websiteUrl.toUri())
                context.startActivity(intent)
            }) {
                Text("My website")
            }
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("Dismiss")
            }
        }
    )
}

