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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.canvas.R
import com.sameerasw.canvas.ui.components.ContributorsCarousel
import com.sameerasw.canvas.ui.components.MadebySameeraswCard
import com.sameerasw.canvas.utils.HapticUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun AboutBottomSheet(
    onDismissRequest: () -> Unit,
    onToggleDeveloperMode: () -> Unit,
    appName: String = stringResource(R.string.app_canvas),
    developerName: String = stringResource(R.string.app_developer_name),
    description: String = stringResource(R.string.app_description)
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "$appName v$versionName", style = MaterialTheme.typography.headlineLarge)
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.avatar),
                contentDescription = "Developer Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            HapticUtil.performClick(haptics)
                            // onToggleDeveloperMode()
                            Toast.makeText(context, "Nothing hidden here ( ´ ▽ ` )", Toast.LENGTH_SHORT).show()
                        }
                    )
            )

            Text(
                text = stringResource(R.string.developed_by_format, developerName),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 3
            ) {
                ActionButton(
                    text = stringResource(R.string.action_website),
                    iconRes = R.drawable.rounded_web_traffic_24,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://sameerasw.com")
                    }
                )

                ActionButton(
                    text = stringResource(R.string.action_view_on_github),
                    iconRes = R.drawable.brand_github,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://github.com/sameerasw/canvas")
                    }
                )

                ActionButton(
                    text = stringResource(R.string.action_contact),
                    iconRes = R.drawable.rounded_mail_24,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        val mailUri = "mailto:mail@sameerasw.com".toUri()
                        val emailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                            putExtra(Intent.EXTRA_SUBJECT, "Hello from Canvas")
                        }
                        try {
                            context.startActivity(
                                Intent.createChooser(
                                    emailIntent,
                                    context.getString(R.string.send_email_chooser_title)
                                )
                            )
                        } catch (e: ActivityNotFoundException) {
                            Log.w("AboutBottomSheet", "No email app available", e)
                            Toast.makeText(context, R.string.error_no_email_app, Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    outlined = true
                )

                ActionButton(
                    text = stringResource(R.string.action_telegram),
                    iconRes = R.drawable.brand_telegram,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://t.me/tidwib")
                    },
                    outlined = true
                )

                ActionButton(
                    text = stringResource(R.string.action_support),
                    iconRes = R.drawable.rounded_heart_smile_24,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://buymeacoffee.com/sameerasw")
                    },
                    outlined = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ContributorsCarousel()

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.label_other_apps),
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
                    text = stringResource(R.string.app_essentials),
                    iconRes = R.drawable.essentials_icon,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://sameerasw.com/essentials")
                    }
                )

                OtherAppButton(
                    text = stringResource(R.string.app_airsync),
                    iconRes = R.drawable.rounded_devices_24,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://sameerasw.com/airsync")
                    }
                )

                OtherAppButton(
                    text = stringResource(R.string.app_zenzero),
                    iconRes = R.drawable.rounded_web_24,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://sameerasw.com/zen")
                    }
                )

                OtherAppButton(
                    text = stringResource(R.string.app_tasks),
                    iconRes = R.drawable.rounded_task_alt_24,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://github.com/sameerasw/tasks")
                    }
                )

                OtherAppButton(
                    text = stringResource(R.string.app_zero),
                    iconRes = R.drawable.outline_highlight_mouse_cursor_24,
                    onClick = {
                        HapticUtil.performClick(haptics)
                        openUrl(context, "https://github.com/sameerasw/Browser")
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            MadebySameeraswCard(
                modifier = Modifier.fillMaxWidth()
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = Modifier.padding(horizontal = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
