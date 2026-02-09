package com.sameerasw.canvas.ui.components.sheets

import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.canvas.R
import com.sameerasw.canvas.ui.components.ContributorsCarousel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AboutBottomSheet(
    onDismissRequest: () -> Unit,
    onToggleDeveloperMode: () -> Unit,
    appName: String = "Canvas",
    developerName: String = "Sameera Wijerathna",
    description: String = "It's a canvas for your imagination."
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (_: Exception) {
        "Unknown"
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "$appName v$versionName",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Image(
                painter = painterResource(id = R.drawable.avatar_t),
                contentDescription = "Developer Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Developer mode toggled", Toast.LENGTH_SHORT).show()
                            onToggleDeveloperMode()
                        }
                    )
            )

            Text(
                text = "Developed by $developerName",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            // Main Action Buttons
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                ActionButton(
                    text = "Website",
                    iconRes = R.drawable.rounded_web_traffic_24,
                    onClick = { openUrl(context, "https://www.sameerasw.com/canvas") }
                )
                ActionButton(
                    text = "GitHub",
                    iconRes = R.drawable.github,
                    onClick = { openUrl(context, "https://github.com/sameerasw/Canvas") }
                )
                ActionButton(
                    text = "Telegram",
                    iconRes = R.drawable.brand_telegram,
                    onClick = { openUrl(context, "https://t.me/tidwib") },
                    outlined = true
                )
                ActionButton(
                    text = "Support",
                    iconRes = R.drawable.rounded_heart_smile_24,
                    onClick = { openUrl(context, "https://buymeacoffee.com/sameerasw") },
                    outlined = true
                )
            }

            
            ContributorsCarousel()


            Text(
                text = "Other Apps",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                OtherAppButton(
                    text = "Essentials",
                    iconRes = R.drawable.essentials_icon,
                    onClick = { openUrl(context, "https://github.com/sameerasw/essentials") }
                )
                OtherAppButton(
                    text = "AirSync",
                    iconRes = R.drawable.rounded_devices_24,
                    onClick = { openUrl(context, "https://play.google.com/store/apps/details?id=com.sameerasw.airsync") }
                )
                OtherAppButton(
                    text = "ZenZero",
                    iconRes = R.drawable.rounded_web_24,
                    onClick = { openUrl(context, "https://sameerasw.com/zen") }
                )
                OtherAppButton(
                    text = "Tasks",
                    iconRes = R.drawable.rounded_task_alt_24,
                    onClick = { openUrl(context, "https://github.com/sameerasw/tasks") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "With ❤️ from 🇱🇰",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun OtherAppButton(
    text: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open link", Toast.LENGTH_SHORT).show()
    }
}
