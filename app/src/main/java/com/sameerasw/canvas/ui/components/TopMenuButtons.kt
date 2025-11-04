package com.sameerasw.canvas.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sameerasw.canvas.R

@Composable
fun TopMenuButtons(
    visible: Boolean,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShare) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_ios_share_24),
                    contentDescription = "Share"
                )
            }

            IconButton(onClick = onSave) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_download_24),
                    contentDescription = "Save"
                )
            }

            IconButton(onClick = onClear) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_cleaning_services_24),
                    contentDescription = "Clear all"
                )
            }

            IconButton(onClick = onSettings) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_settings_24),
                    contentDescription = "Settings"
                )
            }

            IconButton(onClick = onAbout) {
                Icon(
                    painter = painterResource(id = R.drawable.rounded_info_24),
                    contentDescription = "About"
                )
            }
        }
    }
}

