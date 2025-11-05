package com.sameerasw.canvas.ui.components

import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sameerasw.canvas.R
import com.sameerasw.canvas.utils.HapticUtil

@Composable
fun TopOverlayToolbar(
    visible: Boolean,
    menuOpen: Boolean,
    onMenuToggle: () -> Unit,
    onUndo: () -> Unit,
    // new param to control undo visibility
    canUndo: Boolean,
    // existing redo params
    canRedo: Boolean,
    onRedo: () -> Unit,
    menuContent: @Composable () -> Unit
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (menuOpen) 180f else 0f,
        animationSpec = tween(220)
    )

    val haptics = LocalHapticFeedback.current

    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }, animationSpec = tween(260)),
        exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(220))
    ) {
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .zIndex(20f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(220)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .animateContentSize(animationSpec = tween(220)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Undo: only in layout when visible, expand/shrink horizontally with fade
                            androidx.compose.animation.AnimatedVisibility(
                                visible = canUndo,
                                enter = expandHorizontally(animationSpec = tween<IntSize>(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)),
                                exit = shrinkHorizontally(animationSpec = tween<IntSize>(durationMillis = 200, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing))
                            ) {
                                IconButton(onClick = {
                                    HapticUtil.performClick(haptics)
                                    onUndo()
                                }, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_undo_24),
                                        contentDescription = "Undo",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Redo: only in layout when visible
                            androidx.compose.animation.AnimatedVisibility(
                                visible = canRedo,
                                enter = expandHorizontally(animationSpec = tween<IntSize>(durationMillis = 220, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween<Float>(durationMillis = 220, easing = FastOutSlowInEasing)),
                                exit = shrinkHorizontally(animationSpec = tween<IntSize>(durationMillis = 200, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing))
                            ) {
                                IconButton(onClick = {
                                    HapticUtil.performClick(haptics)
                                    onRedo()
                                }, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_redo_24),
                                        contentDescription = "Redo",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(onClick = {
                                HapticUtil.performClick(haptics)
                                onMenuToggle()
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_arrow_forward_ios_24),
                                    contentDescription = "Top menu",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.rotate(arrowRotation)
                                )
                            }

                            menuContent()
                        }
                    }
                }
            }
        }
    }
}
