package com.sameerasw.canvas.ui.components.toptoolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "arrow_rotation"
    )

    // Add bouncy scale animation for toolbar appearance
    val toolbarScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "toolbar_scale"
    )

    // Icon scale animations for hover/press effects
    val undoScale by animateFloatAsState(
        targetValue = if (canUndo) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "undo_scale"
    )

    val redoScale by animateFloatAsState(
        targetValue = if (canRedo) 1f else 0.85f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "redo_scale"
    )

    val haptics = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    ) {
        Surface(
            modifier = Modifier
                .statusBarsPadding()
                .zIndex(20f)
                .graphicsLayer {
                    scaleX = toolbarScale
                    scaleY = toolbarScale
                }
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
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ),
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
                                .animateContentSize(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Undo: only in layout when visible, expand/shrink horizontally with fade
                            AnimatedVisibility(
                                visible = canUndo,
                                enter = expandHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ),
                                exit = shrinkHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 150)
                                )
                            ) {
                                IconButton(
                                    onClick = {
                                        HapticUtil.performClick(haptics)
                                        onUndo()
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .graphicsLayer {
                                            scaleX = undoScale
                                            scaleY = undoScale
                                        }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_undo_24),
                                        contentDescription = "Undo",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Redo: only in layout when visible
                            AnimatedVisibility(
                                visible = canRedo,
                                enter = expandHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ),
                                exit = shrinkHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                ) + fadeOut(
                                    animationSpec = tween(durationMillis = 150)
                                )
                            ) {
                                IconButton(
                                    onClick = {
                                        HapticUtil.performClick(haptics)
                                        onRedo()
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .graphicsLayer {
                                            scaleX = redoScale
                                            scaleY = redoScale
                                        }
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.rounded_redo_24),
                                        contentDescription = "Redo",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    HapticUtil.performClick(haptics)
                                    onMenuToggle()
                                },
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = arrowRotation
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.rounded_arrow_forward_ios_24),
                                    contentDescription = "Top menu",
                                    tint = MaterialTheme.colorScheme.onSurface
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
